package statistics.experiment;

import static java.util.stream.Collectors.toList;
import static statistics.experiment.NaturalOrder1K.convertTestsToMvnCmd;
import static statistics.experiment.NaturalOrder1K.lookupCommit;
import static statistics.experiment.NaturalOrder1K.mapBugToScript;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.JsonObject;
import statistics.experiment.JsonUtils.JSON_TYPE;

public class QuadrantOrder1k {

  static String baseSetupDir = "1k/quadrantOrder/setups";
  static String baseBugInfoFile = "1k/bug/map/bug-map.json";
  static String baseTestList = "1k/quadrantOrder/base-order/test-method-order.txt";
  static String baseLevelsDir = "1k/quadrantOrder/levels";

  private static void genSetups() {
    var allTests = FileUtils.readTextFile(baseTestList).collect(toList());

    Arrays.stream(Paths.get(baseSetupDir).toFile().listFiles())
        .forEach(setup -> {
          var setupSize = Integer.parseInt(setup.getName());
          var bugs = JsonUtils.parseJsonFromLocalFile(baseBugInfoFile, JSON_TYPE.AS_JSON_ARRAY)
              .get().asJsonArray();
          JsonUtils.<JsonObject>toStream(bugs)
              .sorted(Comparator.comparing(
                  jsonObject -> Integer.parseInt(jsonObject.getString("bugId").split("-")[1])))
              .forEach(bug -> {
                var bugId = bug.getString("bugId");
                var failedTestNames = bug.getJsonArray("failed_tests")
                    .getValuesAs(jsonValue -> jsonValue.toString().replace("\"", ""));
                var passedTestsNeeded = setupSize - failedTestNames.size();
                var firstPassedTests = removeFailedTests(allTests, failedTestNames)
                    .subList(0, passedTestsNeeded);
                var failedTestList = findFailedTests(allTests, failedTestNames);
                var finalTestSetupList = Stream
                    .concat(firstPassedTests.stream(), failedTestList.stream()).collect(toList());
                assert failedTestList.size() >= 1;
                assert finalTestSetupList.size() == setupSize;
                assert finalTestSetupList.get(finalTestSetupList.size() - 1).startsWith("*");
                FileUtils
                    .writeTextToFile(finalTestSetupList.stream().collect(Collectors.joining("\n")),
                        Paths.get(setup.getAbsolutePath(), bugId + ".txt").toFile()
                            .getAbsolutePath());
              });
        });
  }

  private static List<String> removeFailedTests(List<String> allTests, List<String> failedTests) {
    return allTests.stream().filter(
        test -> failedTests.stream().noneMatch(test::contains))
        .collect(Collectors.toList());
  }

  private static List<String> findFailedTests(List<String> allTests, List<String> failedTestNames) {
    return allTests.stream()
        .filter(test -> failedTestNames.stream()
            .anyMatch(test::contains))
        .map(test -> "*" + test)
        .collect(Collectors.toList());
  }

  private static void reduce() {
    var scriptHeader = "#!/usr/bin/env bash\n"
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

    Arrays.stream(Objects.requireNonNull(Paths.get(baseSetupDir).toFile().listFiles()))
        .forEach(level -> {
          var bugs = Arrays.stream(Objects.requireNonNull(level.listFiles())).sorted(
              Comparator.comparing(
                  file -> Integer.parseInt(file.getName().split("\\.")[0].split("-")[1])));
          var allBugCmds = scriptHeader +
              bugs.map(bug -> {
                var tests = FileUtils.readTextFile(bug.getAbsolutePath()).collect(toList());
                var cmdTestParams = convertTestsToMvnCmd(tests);
                var bugId = bug.getName().split("\\.")[0];
                var commit = lookupCommit(bugId);
                var scriptString = mapBugToScript(bugId, commit, cmdTestParams, level.getName());
                return scriptString;
              })
                  .collect(Collectors.joining("\n"));
          var targetFilePath = Paths.get(baseLevelsDir, level.getName() + ".sh").toFile()
              .getAbsolutePath();
          FileUtils.writeTextToFile(allBugCmds, targetFilePath);
        });
  }

  public static void extractTime() {
    var baseDir = "1k/quadrantOrder/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(baseDir).toFile().listFiles()))
        .sorted(Comparator.comparing(q -> Integer.parseInt(q.getName())))
        .forEach(q -> {
          Arrays.stream(q.listFiles()).sorted(Comparator
              .comparing(bug -> Integer.parseInt(bug.getName().split("\\.")[0].split("-")[1])))
              .forEach(bug -> {
                var bugTime = FileUtils.readTextFile(bug.getAbsolutePath())
                    .map(line -> Double.parseDouble(line.split(":")[1]))
                    .reduce(0.0, Double::sum);
                System.out.printf("%.2f%n", bugTime);
              });
          System.out.println("\n\n");
        });
  }

  public static void main(String[] args) {
    reduce();
    extractTime();
  }
}
