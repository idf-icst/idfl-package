package statistics.experiment;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static statistics.experiment.CsvResultReducer.massAugumentCsv;
import static statistics.experiment.ResultCombiner.combineResultsToCsv;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import statistics.experiment.JsonUtils.JSON_TYPE;
import statistics.experiment.ResultCombiner.ExperimentType;

public class RealBugSetup {

  private static String rBugDir = "data/real-bugs";
  private static String executionOrderDir = "data/1k-realBugs/execution-order";

  private static int levels = 10;
  private static String jar = "out/artifacts/dfl.jar";
  private static String cd = "flex-event-registration/event-registration-shared";

  private static String timeTakensDirForNatural = "data/1k-realBugs/naturalOrder/timeTakens";
  private static String timeTakensDirForQuadrant = "data/1k-realBugs/quadrantOrder/timeTakens";

  private static String levelDirForNaturalOrder = "data/1k-realBugs/naturalOrder/levels";
  private static String levelDirForQuadrantOrder = "data/1k-realBugs/quadrantOrder/levels";
  private static String levelDirForTimeOrder = "data/1k-realBugs/timeOrder/levels";

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

  private static void genNaturalOrderSetup() {
    IntStream.range(0, levels + 1)
        .forEach(level -> {
          System.out.println("Level " + level);
          var timeTakenAtLevel = new ArrayList<String>();
          var cmds = header + Arrays.stream(
              Objects.requireNonNull(Paths.get(rBugDir).toFile().listFiles()))
              .sorted(
                  Comparator.comparingInt(
                      value -> Integer.parseInt(value.getName().split("-")[1].split("\\.")[0])))
              .map(rBug -> {
                var bugId = rBug.getName().split("\\.")[0];
                var bugInfo = JsonUtils
                    .parseJsonFromLocalFile(rBug.getAbsolutePath(), JSON_TYPE.AS_JSON_OBJECT)
                    .get().asJsonObject();
                var commitId = bugInfo.getString("commit");
                var firstFailedTest = bugInfo.getJsonArray("failed_tests").get(0).toString()
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

          FileUtils.writeTextToFile(timeTakenAtLevel
                  .stream()
                  .sorted(Comparator
                      .comparingInt(value -> Integer.parseInt(value.split(":")[0].trim().split("-")[1])))
                  .collect(Collectors.joining("\n")),
              Paths.get(timeTakensDirForNatural, level + ".txt").toFile().getAbsolutePath());

        });
  }

  private static void genQuadrantOrderSetup() {
    var testSetSize = 1295;
    var levels = Arrays
        .asList(50, 100, testSetSize / 4, testSetSize / 2, testSetSize / 4 * 3, testSetSize);
    levels.stream().forEach(level -> {
      var timeTakenAtLevel = new ArrayList<String>();
      var cmds = header + Arrays.stream(Paths.get(rBugDir).toFile().listFiles())
          .sorted(Comparator.comparingInt(
              value -> Integer.parseInt(value.getName().split("\\.")[0].split("-")[1])))
          .map(bug -> {
            var bugId = bug.getName().split("\\.")[0];
            var bugInfo = JsonUtils
                .parseJsonFromLocalFile(bug.getAbsolutePath(), JSON_TYPE.AS_JSON_OBJECT).get()
                .asJsonObject();
            var commitId = bugInfo.getString("commit");
            var firstFailedTest = bugInfo.getJsonArray("failed_tests").get(0).toString()
                .replace("\"", "");
            var tests = FileUtils.readTextFile(
                Paths.get(executionOrderDir, bugId, "method-order.txt").toFile().getAbsolutePath())
                .collect(toList());
            var allOtherTests = tests.stream()
                .filter(test -> !test.split(":")[0].endsWith(firstFailedTest))
                .collect(toList());
//            System.out.println(allOtherTests.size());
            var firstFailedTestFulId = tests.stream()
                .filter(test -> test.split(":")[0].endsWith(firstFailedTest)).findFirst().get();
//            Collections.shuffle(allOtherTests);
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

      FileUtils.writeTextToFile(timeTakenAtLevel
              .stream()
              .collect(Collectors.joining("\n")),
          Paths.get(timeTakensDirForQuadrant, level + ".txt").toFile().getAbsolutePath());

      FileUtils.writeTextToFile(cmds,
          Paths.get(levelDirForQuadrantOrder, level + ".sh").toFile().getAbsolutePath());
    });

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
      var cmds = header + Arrays.stream(Paths.get(rBugDir).toFile().listFiles())
          .map(bug -> {
            var bugId = bug.getName().split("\\.")[0];
            var extractedTests = extractTestByTime(finalLevel,
                Paths.get(executionOrderDir, bug.getName().split("\\.")[0], "method-order.txt")
                    .toFile().getAbsolutePath());
            var bugInfo = JsonUtils
                .parseJsonFromLocalFile(bug.getAbsolutePath(), JSON_TYPE.AS_JSON_OBJECT).get()
                .asJsonObject();
            var commit = bugInfo.getString("commit");
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

    var cmds = header + Arrays.stream(Paths.get(rBugDir).toFile().listFiles())
        .map(bug -> {
          var bugId = bug.getName().split("\\.")[0];
          var extractedTests = FileUtils.readTextFile(
              Paths.get(executionOrderDir, bug.getName().split("\\.")[0], "method-order.txt")
                  .toFile().getAbsolutePath())
              .collect(toList());
          var bugInfo = JsonUtils
              .parseJsonFromLocalFile(bug.getAbsolutePath(), JSON_TYPE.AS_JSON_OBJECT).get()
              .asJsonObject();
          var commit = bugInfo.getString("commit");
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

  private static void setups(ExperimentMode mode) {
    switch (mode) {
      case All:
        genNaturalOrderSetup();
        genQuadrantOrderSetup();
        genTimeBasedSetup();
        return;
      case Nat:
        genNaturalOrderSetup();
        return;
      case Quad:
        genQuadrantOrderSetup();
        return;
      case Time:
        genTimeBasedSetup();
        return;
    }
  }

  private static void mergeCsvFiles(String csvDir) {
    var content = Arrays.stream(Paths.get(csvDir).toFile().listFiles())
        .filter(file -> file.getName().contains("_au"))
        .sorted(Comparator.comparing(file -> Integer.parseInt(file.getName().split("_")[0])))
        .peek(file -> System.out.println(file.getName()))
        .map(file -> FileUtils.readTextFile(file.getAbsolutePath()).collect(joining("\n")))
        .collect(joining("\n\n"));
    FileUtils
        .writeTextToFile(content, Paths.get(csvDir, "combined_au.csv").toFile().getAbsolutePath());
  }

  enum ExperimentMode {
    Nat,
    Quad,
    Time,
    All
  }

  public static void main(String[] args) throws IOException {
    reset(ExperimentMode.Nat);
//    setups(ExperimentMode.Nat);
    getFinalResultsWorkflow(ExperimentMode.Nat);
  }

  private static void reset(ExperimentMode mode) {
    cleanResults(mode);
    cleanCsvOrLevels(mode, "csv");
    cleanCsvOrLevels(mode, "levels");
  }

  public static void cleanResults(ExperimentMode mode) {
    ResultCombiner.map(mode).forEach(setupType ->
        Arrays
            .stream(Paths.get("data/1k-realBugs", setupType, "results")
                .toFile().listFiles())
            .forEach(d -> Arrays.stream(d.listFiles()).forEach(file -> file.delete()))
    );
  }

  private static void cleanCsvOrLevels(ExperimentMode mode, String csvOrLevels) {
    Stream<String> dirs = Stream.of();
    switch (mode) {
      case Nat:
        dirs = Stream.of("data/1k-realBugs/naturalOrder/" + csvOrLevels);
        break;
      case Quad:
        dirs = Stream
            .of("data/1k-realBugs/quadrantOrder/" + csvOrLevels);
        break;
      case Time:
        dirs = Stream.of("data/1k-realBugs/timeOrder/" + csvOrLevels);
        break;
      case All:
        dirs = Stream.of("data/1k-realBugs/naturalOrder/" + csvOrLevels,
            "data/1k-realBugs/quadrantOrder/" + csvOrLevels,
            "data/1k-realBugs/timeOrder/" + csvOrLevels);
        break;
    }

    dirs.forEach(dir -> Arrays.stream(Paths.get(dir)
        .toFile()
        .listFiles())
        .forEach(file -> file.deleteOnExit()));

  }

  private static void getFinalResultsWorkflow(ExperimentMode mode, String... excludedBugs) {
    combineResultsToCsv(ExperimentType.Real, mode, excludedBugs);

    Stream<String> dirs = Stream.of();
    switch (mode) {
      case Nat:
        dirs = Stream.of("data/1k-realBugs/naturalOrder/csv");
        break;
      case Quad:
        dirs = Stream.of("data/1k-realBugs/quadrantOrder/csv");
        break;
      case Time:
        dirs = Stream.of("data/1k-realBugs/timeOrder/csv");
        break;
      case All:
        dirs = Stream.of("data/1k-realBugs/naturalOrder/csv",
            "data/1k-realBugs/quadrantOrder/csv",
            "data/1k-realBugs/timeOrder/csv");
        break;
    }

    dirs.forEach(csvDir -> {
      massAugumentCsv(csvDir);
      mergeCsvFiles(csvDir);
    });
  }

}
