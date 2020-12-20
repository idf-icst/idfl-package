package statistics.experiment;

import static statistics.experiment.ExpUtils.getCmdString;
import static statistics.experiment.ExpUtils.mapBugToScript;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.json.JsonValue;
import statistics.experiment.JsonUtils.JSON_TYPE;

public class QuadrantSetup {

  private static String baseSetup = "data/10k/naturalOrder/setups";
  private static String destDir = "data/10k/quadrantsOrder";
  private static String bugMapFile = "data/10k/naturalOrder/bug-map.json";

  private static void genQuadrantTestSet(int k) {
    Arrays.stream(Objects.requireNonNull(Paths.get(baseSetup).toFile().listFiles()))
        .forEach(bugId -> {
          var partition = FileUtils.readTextFile(bugId.getAbsolutePath()).collect(
              Collectors.groupingBy(test -> !test.startsWith("*"), Collectors.toList()));
          var failedTests = partition.get(false);
          var passedTests = partition.get(true);
          var neededPassedTestSize = k - failedTests.size();
          var content = Stream
              .concat(IntStream.range(0, neededPassedTestSize / passedTests.size() + 1)
                      .boxed()
                      .flatMap(i -> passedTests.stream())
                      .collect(Collectors.toList())
                      .subList(0, neededPassedTestSize - 1).stream(),
                  failedTests.stream())
              .collect(Collectors.joining("\n"));
          FileUtils.writeTextToFile(content,
              Paths.get(destDir, String.valueOf(k), bugId.getName() + ".txt").toFile()
                  .getAbsolutePath());
        });
  }

  private static void genRunScript(int k) {
    var mvnCmdMap = Arrays.stream(Paths.get(destDir, String.valueOf(k)).toFile().listFiles())
        .collect(Collectors.toMap(bug -> bug.getName().split("\\.")[0], bug -> {
          var testStream = FileUtils.readTextFile(bug.getAbsolutePath())
              .map(line -> (line.startsWith("*") ? line.substring(1) : line));
          return getCmdString(testStream);
        }));

    var header = "#!/usr/bin/env bash\n"
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
        + "}";

    var scriptContent = JsonUtils.parseJsonFromLocalFile(bugMapFile, JSON_TYPE.AS_JSON_ARRAY).get()
        .asJsonArray()
        .getValuesAs(JsonValue::asJsonObject)
        .stream()
        .map(jsonObject -> mapBugToScript(jsonObject, mvnCmdMap.get(jsonObject.getString("bugId"))))
        .collect(Collectors.joining("\n"));

    FileUtils.writeTextToFile(header + "\n\n" + scriptContent,
        Paths.get(destDir, k + ".sh").toFile().getAbsolutePath());
  }

}
