package statistics.profiling;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import statistics.entity.AbstractEntity;
import statistics.entity.ExecutionEntity;
import statistics.entity.Program;
import statistics.entity.TestCase;

import javax.json.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static statistics.profiling.CloverParams.*;

public class CloverDataParser implements ProfilingParser {

  private String programDir;
  private String programPrefix;

  public CloverDataParser setProgramBaseDir(String programBaseDir) {
    programDir = programBaseDir;
    return this;
  }

  public CloverDataParser setProgramPrefix(String programPrefix) {
    this.programPrefix = programPrefix;
    return this;
  }

  private Set<TestCase> extractedTestCases = new HashSet<>();
  private Set<AbstractEntity> extractedEntities;

  //mvn clean install clover:setup test clover:clover => to generate js profiling data files

  public static List<Path> getJsonProfilingDataFiles(String sDir) throws IOException {
    return Files.find(Paths.get(sDir), getMaxDepthSearch(),
        (p, bfa) -> bfa.isRegularFile() && getJsProfilingFileFilter().test(p))
        .collect(Collectors.toList());
  }

  /*
      Return a map from a class to its covering tests
   */
  private CloverDataParser getTestTargetMap(String projectBaseDir, String projectPrefix)
      throws IOException {
    var cloverProfilingData = getJsonProfilingDataFiles(projectBaseDir)
        .stream()
        .flatMap(path -> getTestTargets(path, projectPrefix).entrySet().stream())
        .collect(
            groupingBy(Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.mapping(TestCase::new,
                    Collectors
                        .collectingAndThen(Collectors.reducing(TestCase::merge), Optional::get)
                    )
                )
            )
        )
        .values();

    extractedTestCases.addAll(cloverProfilingData);
    return this;
  }

  private CloverDataParser buildTestSet() throws IOException {
    return getTestTargetMap(programDir, programPrefix);
  }

  private CloverDataParser buildTestSetThen(Function<Set<TestCase>, CloverDataParser> fn)
      throws IOException {
    return fn.apply(buildTestSet().extractedTestCases);
  }

  private CloverDataParser buildEntitySet(Set<TestCase> testCaseSet) {
    extractedEntities = testCaseSet.stream()
        .flatMap(testCase -> {
          testCase.getEntities()
              .forEach(entity -> entity.addTest(testCase, 1));
          return testCase.getEntities().stream();
        })
        .collect(Collectors.collectingAndThen(groupingBy(e -> e, Collectors.reducing((e1, e2) -> {
          ((ExecutionEntity) e1).getExecutionCount()
              .putAll(((ExecutionEntity) e2).getExecutionCount());
          return e1;
        })), m -> m.values().stream().filter(Optional::isPresent).map(Optional::get)
            .collect(Collectors.toSet())));
    return this;
  }

  private final Function<Set<TestCase>, CloverDataParser> buildEntitySet = this::buildEntitySet;

  private static String getClassFQN(Path classPath, String projectPrefix) {
    String classQFN =
        classPath.toString().contains("\\") ? classPath.toString().replaceAll("\\\\", ".")
            .split(projectPrefix)[1]
            : classPath.toString().replaceAll("/", ".").split(projectPrefix)[1];

    return String.format("%s%s", projectPrefix, classQFN.replace(".js", ""));
  }

  @SuppressWarnings("unchecked")
  private static List<JsonObject> extractTestTargets(String line, String fqn,
      String... srcFileLines) {
    var jsonString = line.substring(line.indexOf("{"), line.lastIndexOf("}") + 1);
    try {
      JSONObject jsonObject = (JSONObject) (new JSONParser()).parse(jsonString);
      return (List<JsonObject>) jsonObject.entrySet().stream()
          .map(e -> parseTestFromKeyValueString(e.toString(), fqn))
          .collect(Collectors.toList());
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  private static Map<Integer, List<Integer>> extractCodeCoveredByTestToMapFromTestToLocations(
      String srcFileLines) {
    var codeLines = Arrays.stream(
        srcFileLines.substring(srcFileLines.indexOf("[") + 2, srcFileLines.lastIndexOf("]") - 1)
            .split("], \\["))
        .map(String::trim)
        .collect(toList());

    return IntStream.range(0, codeLines.size())
        .boxed()
        .filter(line -> !codeLines.get(line).equals(""))
        .map(line -> Arrays.stream(codeLines.get(line).split(","))
            .map(String::trim)
            .map(Integer::parseInt)
            .collect(toMap(test -> test, test -> line))
        )
        .flatMap(testToLineMap -> testToLineMap.entrySet().stream())
        .collect(groupingBy(Map.Entry::getKey,
            Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  private static JsonObject combiner(JsonObject testObject,
      Map<Integer, List<Integer>> testToLocationMap) {
    Set<Integer> locations = new HashSet<>(
        testToLocationMap.get(Integer.parseInt(testObject.getString("id").split("_")[1])));

    Set<Integer> locations2 = testObject.getJsonArray("lines")
        .stream()
        .map(jsonValue -> Integer.parseInt(jsonValue.toString()))
        .collect(toSet());

    locations.addAll(locations2);

    var newJsonObject = Json.createObjectBuilder(testObject)
        .add("lines", Json.createArrayBuilder(locations).build());

    return newJsonObject.build();
  }

  public static Map<Integer, JsonObject> getTestTargets(Path javaJsonFilePath,
      String projectPrefix) {
    var fqn = getClassFQN(javaJsonFilePath, projectPrefix);

    try (Stream<String> stringStream = Files.lines(javaJsonFilePath)) {
      var map = stringStream.filter(
          line -> line.startsWith(CLOVER_TEST_TARGETS_STRING.getStringValue()) || line
              .startsWith(CLOVER_SRC_FILE_LINES_STRING.getStringValue()))
          .collect(groupingBy(line -> line.substring(0, line.indexOf("=")).trim()));

      var srcFileLines = extractCodeCoveredByTestToMapFromTestToLocations(
          map.get(CLOVER_SRC_FILE_LINES_STRING.getStringValue()).get(0));

      var targets = extractTestTargets(map.get(CLOVER_TEST_TARGETS_STRING.getStringValue()).get(0),
          fqn);

      return targets.stream()
          .collect
              (
                  toMap
                      (
                          jsonObject -> Integer.parseInt(jsonObject.getString("id").split("_")[1]),
                          jsonObject -> combiner(jsonObject, srcFileLines)
                      )
              );
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    return Map.of();
  }

  private static JsonObject parseTestFromKeyValueString(String entry, String fqn) {
    String testId = entry.split("=")[0].trim();

    JSONObject data;
    try {
      data = (JSONObject) new JSONParser().parse(entry.split("=")[1].trim());
    } catch (ParseException e) {
      e.printStackTrace();
      return JsonValue.EMPTY_JSON_OBJECT;
    }
    String testName = data.get("name").toString();
    String testStatus = data.get("pass").toString().equals("true") ? "passed" : "failed";
    boolean passed = Boolean.parseBoolean(data.get("pass").toString());

    JsonArrayBuilder lines = Json.createArrayBuilder();
    Arrays.stream(((JSONArray) data.get("statements")).toArray())
        .map(e -> Integer.parseInt(((JSONObject) e).get("sl").toString()))
        .forEach(lines::add);

    JsonArrayBuilder coveredMethods = Json.createArrayBuilder();
    Arrays.stream(((JSONArray) data.get("methods")).toArray())
        .map(e -> Integer.parseInt(((JSONObject) e).get("sl").toString()))
        .forEach(coveredMethods::add);

    return Json.createObjectBuilder()
        .add("FQN", fqn)
        .add("id", testId)
        .add("name", testName)
        .add("status", testStatus)
        .add("passed", passed)
        .add("lines", lines)
        .add("methods", coveredMethods)
        .build();
  }

  private Program thenBuildProgram() {
    var program = new Program(extractedEntities, extractedTestCases);
    program.setName(programPrefix);
    program.setCodeBase(programPrefix);
    program.setVersion(programPrefix);
    return program;
  }

  @Override
  public Optional<Program> toProgram() {
    try {
      return Optional.of(buildTestSetThen(buildEntitySet).thenBuildProgram());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }
}
