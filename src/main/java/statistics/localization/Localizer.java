package statistics.localization;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static statistics.algorithm.RankingAlgorithm.TARANTULA;
import static statistics.entity.AnalysisLevel.STATEMENT;
import static statistics.experiment.JsonUtils.parseJsonFromLocalFile;
import static statistics.experiment.JsonUtils.writeJsonObjectToFile;
import static statistics.profiling.CloverParams.DEFAULT_PROJECT_DIR;
import static statistics.profiling.CloverParams.DEFAULT_PROJECT_PREFIX;
import static statistics.profiling.CloverParams.TOP_K;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import statistics.algorithm.RankingAlgorithm;
import statistics.entity.AnalysisLevel;
import statistics.entity.Program;
import statistics.profiling.CloverDataParser;

/**
 * This is a main class that uses a ranker which accepts a program and ranks all elements in the
 * program
 */
public class Localizer {

  private static Optional<Program> parseToProgram(String programDir, String program_prefix) {
    return new ProgramBuilder().setProfilingParser
        (
            new CloverDataParser()
                .setProgramBaseDir(programDir == null || programDir.isEmpty() ? DEFAULT_PROJECT_DIR
                    .getStringValue() : programDir)
                .setProgramPrefix(
                    program_prefix == null || program_prefix.isEmpty() ? DEFAULT_PROJECT_PREFIX
                        .getStringValue() : program_prefix)
        )
        .build();
  }

  private static void localize(Program program) {
    Optional.of(SpectrumBasedLocalizer.accept(program)
        .setAlgorithm(TARANTULA)
        .setAnalysisLevel(STATEMENT)
        .setTopK(TOP_K.getIntValue())
        .localizeBug()
        .reportToJson())
        .ifPresent(System.out::println);
  }

  public static void localize(String programDir, String program_prefix) {
    parseToProgram(programDir, program_prefix)
        .ifPresent(Localizer::localize);
  }

  private enum BugCategory {
    Real,
    Injected
  }

  public static void main(String[] args) {
    var bugId = args[0];
    var level = args[1];
    var type = args[2];

    System.out.println("running with these params " + bugId + " " + level + " " + type + "\n");

    forEvaluation(DEFAULT_PROJECT_DIR.getStringValue(), DEFAULT_PROJECT_PREFIX.getStringValue(),
        null, true,
        "STATEMENT", 100, false, bugId, level, true, false, type, BugCategory.Injected);
  }

  public static void forEvaluation(String profilingDir, String projectPrefix,
      String rankingAlgorithms, boolean useAllRankingAlgorithms,
      String localizingLevel, int topK, boolean reportToDBA, String bugId, String level,
      boolean isEvaluation, boolean includedProfiling, String type, BugCategory bugCategory) {

    var resultDir = "";
    if (bugCategory == BugCategory.Real) {
      switch (type) {
        case "natural":
          resultDir = "data/1k-realBugs/naturalOrder/results";
          break;
        case "quadrant":
          resultDir = "data/1k-realBugs/quadrantOrder/results";
          break;
        case "time":
          resultDir = "data/1k-realBugs/timeOrder/results";
          break;
        default:
          break;
      }
    } else if (bugCategory == BugCategory.Injected) {
      switch (type) {
        case "natural":
          resultDir = "data/1k/naturalOrder/results";
          break;
        case "quadrant":
          resultDir = "data/1k/quadrantOrder/results";
          break;
        case "time":
          resultDir = "data/1k/timeOrder/results";
          break;
        default:
          break;
      }
    }

    var bugInfoDir = "";

    if (bugCategory == BugCategory.Real) {
      bugInfoDir = "data/real-bugs";
    } else if (bugCategory == BugCategory.Injected) {
      bugInfoDir = "data/bugs";
    }

    var bugInfo = bugInfoDir + "/" + bugId + ".json";

    final String DOC_ID_PREFIX = "dfl.software";

    SpectrumBasedLocalizer spectrumBasedLocalizer = SpectrumBasedLocalizer.accept(
        parseToProgram(profilingDir, projectPrefix).orElseThrow(IllegalArgumentException::new));

    JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
    String docId;

    if (isEvaluation) {
      jsonObjectBuilder.add("bug-info", parseJsonFromLocalFile(bugInfo).map(JsonValue::asJsonObject)
          .orElse(JsonValue.EMPTY_JSON_OBJECT));
      docId = DOC_ID_PREFIX + "::" + projectPrefix + "::" + bugId + "::" + DateTimeUtils
          .getCurrentUnixTime();
    } else {
      docId = DOC_ID_PREFIX + "::" + projectPrefix + "::" + DateTimeUtils.getCurrentUnixTime();
    }

    jsonObjectBuilder.add("evals",
        (useAllRankingAlgorithms ? Arrays.stream(RankingAlgorithm.values()).collect(toList())
            : Arrays.stream(rankingAlgorithms.split(","))
                .map(algo -> RankingAlgorithm.valueOf(algo.toUpperCase()))
                .collect(toList())).stream()
            .map(algorithm -> spectrumBasedLocalizer
                .setAlgorithm(algorithm)
                .setAnalysisLevel(AnalysisLevel.valueOf(localizingLevel.toUpperCase()))
                .setTopK(topK)
                .localizeBug()
                .reportToJson())
            .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
            .build());

    if (includedProfiling) {
      jsonObjectBuilder.add("profile", spectrumBasedLocalizer.getProgram().toProfile());
    }

    if (!Paths.get(resultDir, level).toFile().exists()) {
      Paths.get(resultDir, level).toFile().mkdir();
    }

    writeJsonObjectToFile(jsonObjectBuilder.build(),
        Paths.get(resultDir, level, docId + ".json").toFile().getAbsolutePath());
  }

  private static void updateMetaReposJsonObjectFile(String rootDir, String filePath)
      throws IOException {
    var repos = extractRepoMetaDta(rootDir).values().parallelStream()
        .map(Json::createObjectBuilder)
        .map(jsonObjectBuilder -> jsonObjectBuilder.remove("dir"))
        .map(jsonObjectBuilder -> jsonObjectBuilder.add("version", "*"))
        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::addAll);

    writeJsonObjectToFile(Json.createObjectBuilder()
        .add("repos", repos)
        .build(), filePath);
  }

  private static Map<String, JsonObject> extractRepoMetaDta(String rootDir) throws IOException {
    Function<Path, Path> findId = project -> {
      try {
        return Files.walk(project, 1000)
            .filter(path -> path.endsWith("pkg-summary.html"))
            .map(Path::getParent)
            .sorted()
            .findFirst()
            .orElseThrow(IOException::new);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return Paths.get(".", "unknown");
    };

    return Files.list(Paths.get(rootDir))
        .filter(path -> !path.toFile().getName().startsWith("."))
        .map(project -> {
          var dir = findId.apply(project.resolve("com").resolve("cvent"));
          return Json.createObjectBuilder()
              .add("dir", dir.toAbsolutePath().toString())
              .add("id", project.relativize(dir).toString().replace("/", "."))
              .add("name", project.getFileName().toString())
              .build();
        })
        .collect(toMap(jsonObject -> jsonObject.getString("id"), jsonObject -> jsonObject,
            (o, o2) -> o));
  }
}
