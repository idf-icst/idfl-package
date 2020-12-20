package statistics.experiment;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static statistics.experiment.NaturalOrder1K.convertTestsToMvnCmd;
import static statistics.experiment.NaturalOrder1K.mapBugToScript;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.json.JsonObject;
import statistics.experiment.JsonUtils.JSON_TYPE;

public class Frequency1k {

  private static String allTestList = "data/1k/naturalOrder/test-in-order/test-method-order.txt";
  private static String baseSetupDir = "data/1k/frequency-based/setups";
  private static String baseLevelsDir = "data/1k/frequency-based/levels";
  private static String bugMapPath = "data/1k/bug/map/bug-map.json";

  public static double calculateTimeTaken(List<String> tests) {
    return tests.stream().map(test -> Double.parseDouble(test.split(":")[1]))
        .reduce(0.0, Double::sum);
  }

  public static List<String> findTestsByTime(double upperBound) {
    var allTests = FileUtils.readTextFile(allTestList).collect(toList());
    var results = new ArrayList<String>();
    var index = -1;
    var totalTime = 0.0;
    while (index < allTests.size() - 1
        && (totalTime += Double.parseDouble(allTests.get(++index).split(":")[1])) < upperBound) {
      results.add(allTests.get(index));
    }
    return results;
  }

  public static void prepareSetupsForFrequencyBased() {
    var frequency = 120.00;// second
    var totalTime = calculateTimeTaken(
        FileUtils.readTextFile(allTestList).collect(Collectors.toList()));
    var subTotal = 0.0;
    var baseSetupDir = "data/1k/frequency-based/setups";

    while (subTotal < totalTime) {
      subTotal += frequency;
      var tests = findTestsByTime(subTotal).stream().collect(joining("\n"));
      var setupName = Double.valueOf(subTotal / 60).intValue();
      FileUtils.writeTextToFile(tests,
          Paths.get(baseSetupDir, setupName + ".txt").toFile().getAbsolutePath());
    }
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

          var bugs = JsonUtils.parseJsonFromLocalFile(bugMapPath, JSON_TYPE.AS_JSON_ARRAY).get()
              .asJsonArray();

          var allBugCmds = scriptHeader + JsonUtils.<JsonObject>toStream(bugs)
              .sorted(Comparator.comparing(
                  jsonObject -> Integer.parseInt(jsonObject.getString("bugId").split("-")[1])))
              .map(bug -> {
                var tests = FileUtils.readTextFile(level.getAbsolutePath()).collect(toList());
                var cmdTestParams = convertTestsToMvnCmd(tests);
                var bugId = bug.getString("bugId");
                var commit = bug.getString("commit");
                return mapBugToScript(bugId, commit, cmdTestParams,
                    level.getName().split("\\.")[0]);
              })
              .collect(Collectors.joining("\n"));

          var targetFilePath = Paths.get(baseLevelsDir, level.getName().split("\\.")[0] + ".sh")
              .toFile()
              .getAbsolutePath();

          FileUtils.writeTextToFile(allBugCmds, targetFilePath);
        });
  }

  public static void main(String[] args) {
    reduce();
  }

}
