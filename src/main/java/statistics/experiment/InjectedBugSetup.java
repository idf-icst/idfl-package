package statistics.experiment;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static statistics.experiment.CsvResultReducer.massAugumentCsv;
import static statistics.experiment.ResultCombiner.combineResultsToCsv;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.json.JsonObject;
import statistics.experiment.JsonUtils.JSON_TYPE;
import statistics.experiment.RealBugSetup.ExperimentMode;
import statistics.experiment.ResultCombiner.ExperimentType;

public class InjectedBugSetup {

  private static String executionOrderDir = "data/1k/execution-order";
  private static String iBugDir = "data/bugs";
  private static String jar = "out/artifacts/dfl.jar";

  private static String levelDirForNaturalOrder = "data/1k/naturalOrder/levels";
  private static String timeTakensDirForNatural = "data/1k/naturalOrder/timeTakens";

  private static String timeTakensDirForQuadrant = "data/1k/quadrantOrder/timeTakens";
  private static String levelDirForQuadrantOrder = "data/1k/quadrantOrder/levels";

  private static String levelDirForTimeOrder = "data/1k/timeOrder/levels";

  private static String cd = "flex-event-registration/event-registration-service-web";

  private static String bugMap = "data/1k/bug-map.json";

  private static String setupDir = "data/1k/naturalOrder/setups";

  private static String header = "#!/usr/bin/env bash\n"
      + "\n"
      + "function setjdk() {\n"
      + "  if [ $# -ne 0 ]; then\n"
      + "    removeFromPath '/System/Library/Frameworks/JavaVM.framework/Home/bin'\n"
      + "    if [ -n \"${JAVA_HOME+x}\" ]; then\n"
      + "      removeFromPath $JAVA_HOME\n"
      + "    fi\n"
      + "    export JAVA_HOME=`/usr/libexec/java_home -v $@`\n"
      + "    export PATH=$JAVA_HOME/bin:$PATH\n"
      + "  fi\n"
      + "}\n"
      + "\n"
      + "removeFromPath () {\n"
      + "    export PATH=$(echo $PATH | sed -E -e \"s;:$1;;\" -e \"s;$1:?;;\")\n"
      + "}\n\n";

  private static String mapToMvnCmd(String cd, String checkoutCommit, String testParams,
      String bugId, String level, String type) {
    return "cd " + cd + ";\n" +
        "git checkout " + checkoutCommit + ";\n" +
        "setjdk 1.8;\n" +
        "mvn clean clover:instrument clover:clover -Dmaven.test.failure.ignore=true -Dfindbugs.skip=true -Denforcer.skip=true -Dtest="
        + testParams + ";\n" +
        "setjdk 11;\n" +
        "java -jar " + jar + " " + bugId + " " + level + " " + type + ";\n";
  }

  private static void createExecutionOrder() {
    var bugMapDir = "data/1k/bug-map.json";
    JsonUtils.<JsonObject>toStream(
        JsonUtils.parseJsonFromLocalFile(bugMapDir, JSON_TYPE.AS_JSON_ARRAY).get().asJsonArray())
        .map(bug -> bug.getString("bugId"))
        .forEach(bugId -> {
          Paths.get(executionOrderDir, bugId).toFile().mkdir();
          FileUtils.writeTextToFile(FileUtils.readTextFile(
              "data/1k.bak/naturalOrder/test-in-order/test-method-order.txt")
                  .collect(Collectors.joining("\n")),
              Paths.get(executionOrderDir, bugId, "method-order.txt").toFile().getAbsolutePath());
        });
  }

  private static void genNaturalOrderSetup() {
    var levels = 10;
    IntStream.range(0, levels + 1)
        .forEach(level -> {
          var timeTakenAtLevel = new ArrayList<String>();
          var cmds = header + JsonUtils.<JsonObject>toStream(
              JsonUtils.parseJsonFromLocalFile(bugMap, JSON_TYPE.AS_JSON_ARRAY).get().asJsonArray())
              .sorted(Comparator.comparing(
                  jsonObject -> Integer.parseInt(jsonObject.getString("bugId").split("-")[1])))
              .map(bug -> {
                var bugId = bug.getString("bugId");
                System.out.println(bugId);

                var commitId = bug.getString("commit");

                var firstFailedTest = bug.getJsonArray("failed_tests").get(0).toString()
                    .replace("\"", "");

                var tests = FileUtils.readTextFile(
                    Paths.get(executionOrderDir, bugId, "method-order.txt").toFile()
                        .getAbsolutePath())
                    .collect(toList());

                var failedTestIndex = IntStream.range(0, tests.size())
                    .filter(j -> tests.get(j).split(":")[0].endsWith(firstFailedTest))
                    .findFirst()
                    .getAsInt();

                var testSet = tests.subList(0, failedTestIndex + level + 1);

                var timeTaken = testSet.stream().map(test -> Double.parseDouble(test.split(":")[1]))
                    .reduce(0.0, Double::sum);

                timeTakenAtLevel.add(bugId + ": " + timeTaken);

                var dTestParams = testSet.stream().map(line -> line.split(":")[0])
                    .collect(groupingBy(test -> test.split("#")[0],
                        Collectors.mapping(test -> test.split("#")[1], joining("+"))))
                    .entrySet()
                    .stream()
                    .map(e -> e.getKey() + "#" + e.getValue())
                    .collect(joining(","));

                return mapToMvnCmd(cd, commitId, dTestParams, bugId, String.valueOf(level),
                    "natural");
              })
              .collect(Collectors.joining("\n"));

          FileUtils.writeTextToFile(cmds,
              Paths.get(levelDirForNaturalOrder, level + ".sh").toFile().getAbsolutePath());

          FileUtils.writeTextToFile(String.join("\n", timeTakenAtLevel),
              Paths.get(timeTakensDirForNatural, level + ".txt").toFile().getAbsolutePath());

        });
  }

  private static void genQuadrantOrderSetup() {
    var testSetSize = 1295;
    var levels = Arrays
        .asList(50, 100, testSetSize / 4, testSetSize / 2, testSetSize / 4 * 3, testSetSize);
    levels.forEach(level -> {
      var timeTakenAtLevel = new ArrayList<String>();
      var cmds = header + JsonUtils.<JsonObject>toStream(
          JsonUtils.parseJsonFromLocalFile(bugMap, JSON_TYPE.AS_JSON_ARRAY).get().asJsonArray())
          .sorted(Comparator.comparing(
              jsonObject -> Integer.parseInt(jsonObject.getString("bugId").split("-")[1])))
          .map(bug -> {
            var bugId = bug.getString("bugId");

            var commitId = bug.getString("commit");
            var firstFailedTest = bug.getJsonArray("failed_tests").get(0).toString()
                .replace("\"", "");
            var tests = FileUtils.readTextFile(
                Paths.get(executionOrderDir, bugId, "method-order.txt").toFile().getAbsolutePath())
                .collect(toList());
            var allOtherTests = tests.stream()
                .filter(test -> !test.split(":")[0].endsWith(firstFailedTest))
                .collect(toList());
            System.out.println(allOtherTests.size());
            var firstFailedTestFulId = tests.stream()
                .filter(test -> test.split(":")[0].endsWith(firstFailedTest)).findFirst().get();
            var passedTestList = allOtherTests.subList(0, level - 2);
            var finalTestList = Stream.of(passedTestList, List.of(firstFailedTestFulId))
                .flatMap(List::stream).collect(toList());
            var timeTaken = finalTestList.stream()
                .map(test -> Double.parseDouble(test.split(":")[1]))
                .reduce(0.0, Double::sum);

            timeTakenAtLevel.add(bugId + ": " + timeTaken);

            var dTestParams = finalTestList.stream().map(line -> line.split(":")[0])
                .collect(groupingBy(test -> test.split("#")[0],
                    Collectors.mapping(test -> test.split("#")[1], joining("+"))))
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "#" + e.getValue())
                .collect(joining(","));

            return mapToMvnCmd(cd, commitId, dTestParams, bugId, String.valueOf(level), "quadrant");
          })
          .collect(Collectors.joining("\n"));

      FileUtils.writeTextToFile(String.join("\n", timeTakenAtLevel),
          Paths.get(timeTakensDirForQuadrant, level + ".txt").toFile().getAbsolutePath());

      FileUtils.writeTextToFile(cmds,
          Paths.get(levelDirForQuadrantOrder, level + ".sh").toFile().getAbsolutePath());
    });

  }

  private static List<String> extractTestByTime(double timeAllowed,
      String testMethodOrderFilePath) {
    var tests = FileUtils.readTextFile(testMethodOrderFilePath).collect(toList());
    var timeSoFar = 0.0;
    var index = -1;
    var accumulatedTests = new ArrayList<String>();

    while (timeSoFar < timeAllowed) {
      var currentTest = tests.get(++index);
      accumulatedTests.add(currentTest);
      timeSoFar += Double.parseDouble(currentTest.split(":")[1]);
    }
    return accumulatedTests;
  }

  private static void genTimeBasedSetup() {
    var maxTimeConsumed = Arrays.stream(Paths.get(executionOrderDir).toFile().listFiles())
        .map(bug ->
            FileUtils.readTextFile(
                Paths.get(bug.getAbsolutePath(), "method-order.txt").toFile().getAbsolutePath())
                .map(line -> Double.parseDouble(line.split(":")[1]))
                .reduce(0.0, (a, b) -> a + b)
        )
        .max(Comparator.naturalOrder())
        .get();

    var frequency = 120.00;
    var level = frequency;
    while (level < maxTimeConsumed) {
      double finalLevel = level;
      var cmds = header + JsonUtils.<JsonObject>toStream(
          JsonUtils.parseJsonFromLocalFile(bugMap, JSON_TYPE.AS_JSON_ARRAY).get().asJsonArray())
          .sorted(Comparator.comparing(
              jsonObject -> Integer.parseInt(jsonObject.getString("bugId").split("-")[1])))
          .map(bug -> {
            var bugId = bug.getString("bugId");
            var extractedTests = extractTestByTime(finalLevel,
                Paths.get(executionOrderDir, bugId, "method-order.txt").toFile().getAbsolutePath());

            var commit = bug.getString("commit");

            var dTestParams = extractedTests.stream().map(line -> line.split(":")[0])
                .collect(groupingBy(test -> test.split("#")[0],
                    Collectors.mapping(test -> test.split("#")[1], joining("+"))))
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "#" + e.getValue())
                .collect(joining(","));
            return mapToMvnCmd(cd, commit, dTestParams, bugId,
                String.valueOf(Math.round(finalLevel)), "time");
          })
          .collect(Collectors.joining("\n"));
      FileUtils.writeTextToFile(cmds,
          Paths.get(levelDirForTimeOrder, Math.round(finalLevel) + ".sh").toFile()
              .getAbsolutePath());
      level += frequency;
    }

    var cmds = header + JsonUtils.<JsonObject>toStream(
        JsonUtils.parseJsonFromLocalFile(bugMap, JSON_TYPE.AS_JSON_ARRAY).get().asJsonArray())
        .map(bug -> {
          var bugId = bug.getString("bugId");
          var extractedTests = FileUtils.readTextFile(
              Paths.get(executionOrderDir, bugId, "method-order.txt").toFile().getAbsolutePath())
              .collect(toList());

          var commit = bug.getString("commit");
          var dTestParams = extractedTests.stream().map(line -> line.split(":")[0])
              .collect(groupingBy(test -> test.split("#")[0],
                  Collectors.mapping(test -> test.split("#")[1], joining("+"))))
              .entrySet()
              .stream()
              .map(e -> e.getKey() + "#" + e.getValue())
              .collect(joining(","));
          return mapToMvnCmd(cd, commit, dTestParams, bugId,
              String.valueOf(Math.round(maxTimeConsumed)), "time");
        })
        .collect(Collectors.joining("\n"));
    FileUtils.writeTextToFile(cmds,
        Paths.get(levelDirForTimeOrder, Math.round(maxTimeConsumed) + ".sh").toFile()
            .getAbsolutePath());

  }

  private static void reset(String runMode) {
    var resultsDir = ResultCombiner.findDirs.apply(ExperimentType.Injected, runMode)
        .get("source");
    var csvDir = ResultCombiner.findDirs.apply(ExperimentType.Injected, runMode)
        .get("dest");
    var levelsDir = ResultCombiner.findDirs.apply(ExperimentType.Injected, runMode)
        .get("levels");

    Stream.concat(
        Arrays.stream(Objects.requireNonNull(Paths.get(resultsDir).toFile().listFiles())).map(
            File::getAbsolutePath), Stream.of(csvDir, levelsDir))
        .forEach(dir -> Arrays.stream(Objects.requireNonNull(Paths.get(dir).toFile().listFiles()))
            .forEach(File::deleteOnExit));
  }

  private static void setups() {
    genNaturalOrderSetup();
    genQuadrantOrderSetup();
    genTimeBasedSetup();
  }

  private static void getFinalResultsWorkflow(ExperimentMode mode, String... excludedBugs) {
    combineResultsToCsv(ExperimentType.Injected, mode, excludedBugs);

    Stream<String> dirs = Stream.of();
    switch (mode) {
      case Nat:
        dirs = Stream.of("data/1k/naturalOrder/csv");
        break;
      case Quad:
        dirs = Stream.of("data/1k/quadrantOrder/csv");
        break;
      case Time:
        dirs = Stream.of("data/1k/timeOrder/csv");
        break;
      case All:
        dirs = Stream.of("data/1k/naturalOrder/csv",
            "data/1k/quadrantOrder/csv",
            "data/1k/timeOrder/csv");
        break;
    }

    dirs.forEach(csvDir -> {
      massAugumentCsv(csvDir);
    });
  }

  private static void calTakenTime(String bugId) {
    var setupInfoPath = Arrays.stream(Paths.get(setupDir).toFile().listFiles())
        .filter(file -> file.getName().startsWith(bugId))
        .findFirst()
        .map(File::getAbsolutePath)
        .orElseThrow();

    var tests = FileUtils.readTextFile(setupInfoPath).collect(Collectors.toList());

    Function<List<String>, Double> cal = list -> list.stream()
        .map(test -> Double.valueOf(test.split(":")[1]))
        .map(time -> new Random().nextDouble() / 150 + time)
        .reduce(0.0, (a, b) -> a + b);

    IntStream.range(0, tests.size())
        .filter(i -> tests.get(i).startsWith("*"))
        .boxed()
        .map(i -> i + 1)
        .limit(9)
        .forEach(i -> System.out.println(i + ": " + cal
            .apply(IntStream.range(0, i).boxed().map(tests::get).collect(Collectors.toList()))));
  }

  public static void main(String[] args) {
    createExecutionOrder();
    setups();
    reset("naturalOrder");
    getFinalResultsWorkflow(ExperimentMode.Nat);
  }
}
