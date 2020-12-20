package statistics.experiment;

import java.io.File;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import statistics.algorithm.RankingAlgorithm;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import statistics.experiment.RealBugSetup.ExperimentMode;

import static java.util.stream.Collectors.*;

public class ResultCombiner {

  enum ExperimentType {
    Real,
    Injected,
    Mimic
  }

  static BiFunction<ExperimentType, String, Map<String, String>> findDirs = (expMode, runMode) -> {
    switch (expMode) {
      case Real:
        return Map.of("source", String
                .format("data/1k-realBugs/%s/results", runMode),
            "dest", String
                .format("data/1k-realBugs/%s/csv", runMode));
      case Mimic:
        return Map.of("source", String
                .format("data/1k-mimicBugs/%s/results", runMode),
            "dest", String
                .format("data/1k-mimicBugs/%s/csv", runMode));
      case Injected:
        return Map.of("source", String
                .format("data/1k/%s/results", runMode),
            "dest", String
                .format("data/1k/%s/csv", runMode),
            "levels", String
                .format("data/1k/%s/levels", runMode));
      default:
        return Map.of();
    }
  };

  private static void removeDir(String dirPath) {
    Arrays.stream(Objects.requireNonNull(Paths.get(dirPath).toFile().listFiles())).forEach(
        File::delete);
  }

  public static Stream<String> map(ExperimentMode experimentMode) {
    Stream<String> experimentModes = Stream.of();
    switch (experimentMode) {
      case All:
        experimentModes = Stream.of("naturalOrder", "quadrantOrder", "timeOrder");
        break;
      case Nat:
        experimentModes = Stream.of("naturalOrder");
        break;
      case Quad:
        experimentModes = Stream.of("quadrantOrder");
        break;
      case Time:
        experimentModes = Stream.of("timeOrder");
        break;
    }
    return experimentModes;
  }

  public static void combineResultsToCsv(ExperimentType experimentType,
      ExperimentMode experimentMode, String... excludeBugs) {

    Predicate<String> filter = s -> excludeBugs == null || Arrays.stream(excludeBugs)
        .noneMatch(s::contains);

    map(experimentMode).forEach(mode -> {
      var sourceDir = findDirs.apply(experimentType, mode).get("source");
      var destDir = findDirs.apply(experimentType, mode).get("dest");
      removeDir(destDir);
      Arrays.stream(Objects.requireNonNull(Paths.get(sourceDir).toFile().listFiles()))
          .filter(file -> filter.test(file.getName()))
          .forEach(file -> {
            FileUtils.writeTextToFile(
                String.join("\n", combineResults(
                    Paths.get(sourceDir, file.getName()).toFile().getAbsolutePath())),
                String.format(destDir + "/%s.csv", file.getName())
            );
          });
    });

  }

  public static void main(String[] args) {
    ExperimentType experimentMode = ExperimentType.Real;
    combineResultsToCsv(ExperimentType.Real, ExperimentMode.All);
    extractTime();
    combineResults("data/1k/naturalOrder/results/level-0");
  }

  public static List<String> combineResults(String resultDir) {
    return Stream.concat(Stream.of(Stream.concat(
        Stream.of("Algorithm"),
        Arrays.stream(RankingAlgorithm.values())
            .map(Enum::name))
            .collect(joining(","))),
        Arrays.stream(Objects.requireNonNull(Paths.get(resultDir).toFile().listFiles()))
            .sorted(Comparator
                .comparing(file -> Integer.valueOf(file.getName().split("::")[2].split("-")[1])))
            .map(File::getAbsolutePath)
            .map(ResultCombiner::process))
        .collect(toList());
  }

  public static int findRank(List<String> groundTruth, Map<Set<String>, Integer> rankingMap) {
    return rankingMap.entrySet().stream()
        .filter(e -> groundTruth.stream().map(name -> "STATEMENT:" + name)
            .anyMatch(entity -> e.getKey().contains(entity)))
        .map(Entry::getValue)
        .findFirst()
        .orElse(201);
  }

  @SuppressWarnings("unchecked")
  public static String process(String bugFileName) {
    var bugId = bugFileName.split("::")[2];

    var result = JsonUtils.readJsonFromFile(bugFileName);
    var bugInfo = (JSONObject) result.get("bug-info");
    var groundTruth = bugInfo.get("locations").toString().split(":");
    var fqn = groundTruth[0];
    var locations = Arrays.stream(groundTruth).skip(1)
        .map(number -> fqn + ":" + number)
        .collect(toList());

    var algos = (JSONArray) result.get("evals");

    return bugId + "," + algos.stream()
        .map(algo -> {
          var rankings = (Map<Double, Set<String>>) ((JSONArray) ((JSONObject) algo)
              .get("entities")).stream()
              .collect(
                  groupingBy(jsonObject -> Double
                          .valueOf(((JSONObject) jsonObject).get("score").toString()),
                      mapping(jsonObject -> ((JSONObject) jsonObject).get("entity_name"),
                          toSet())));

          var values = rankings.entrySet().stream()
              .sorted((o1, o2) -> -o1.getKey().compareTo(o2.getKey()))
              .map(Entry::getValue).collect(toList());

          var rankingMap = IntStream.range(0, values.size() - 1).boxed()
              .collect(toMap(values::get, i -> i + 1));

          var foundRanking = findRank(locations, rankingMap);

          return foundRanking < 201 ? String.valueOf(foundRanking) : ">100";
        })
        .collect(joining(","));
  }

  public static void extractTime() {
    var dir = "src/main/exp-data/tests/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(dir).toFile().listFiles()))
        .sorted(Comparator
            .comparing(file -> Integer.valueOf(file.getName().split("\\.")[0].split("-")[1])))
        .forEach(file -> {
          System.out.println(FileUtils.readTextFile(file.getAbsolutePath()).collect(toList()).get(1)
              .split(":")[1]);
        });
  }
}
