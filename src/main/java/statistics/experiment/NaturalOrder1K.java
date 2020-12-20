package statistics.experiment;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import statistics.experiment.JsonUtils.JSON_TYPE;

public class NaturalOrder1K {

  //  private static String bugMapPath = "data/1k/bug/map/bug-map.json";
  private static String bugMapPath = "data/1k/bug-map.json";
  private static String allTestList = "data/1k/naturalOrder/test-in-order/test-method-order.txt";
  private static String setupDir = "data/1k/naturalOrder/setups";
  private static String levelDir = "data/1k/naturalOrder/levels";
  private static String resultDir = "data/1k/naturalOrder/results";

  private static List<Integer> levels = IntStream.range(0, 11).boxed()
      .collect(Collectors.toUnmodifiableList());

  private static void generateSetups() {

    var allTests = FileUtils.readTextFile(allTestList).collect(toList());
    var allBugs = JsonUtils.parseJsonFromLocalFile(bugMapPath, JSON_TYPE.AS_JSON_ARRAY).get()
        .asJsonArray();

    JsonUtils.<JsonObject>toStream(allBugs)
        .sorted(Comparator
            .comparing(jsonObject -> Integer.parseInt(jsonObject.getString("bugId").split("-")[1])))
        .forEach(bug -> {
          var failedTests = bug.getJsonArray("failed_tests");

          var markedTests = allTests.stream().map(test -> {
            return JsonUtils.<JsonValue>toStream(failedTests)
                .map(JsonValue::toString)
                .map(s -> s.replace("\"", ""))
                .anyMatch(test::contains)
                ? "*" + test
                : test;
          })
              .collect(Collectors.joining("\n"));

          assert markedTests.contains("*");

          var setupFileName = bug.getString("bugId") + ".txt";

          FileUtils.writeTextToFile(markedTests,
              Paths.get(setupDir, setupFileName).toFile().getAbsolutePath());
        });
  }

  public static String lookupCommit(String bugId) {
    var allBugs = JsonUtils.parseJsonFromLocalFile(bugMapPath, JSON_TYPE.AS_JSON_ARRAY).get()
        .asJsonArray();

    return JsonUtils.<JsonObject>toStream(allBugs)
        .filter(jsonObject -> jsonObject.getString("bugId").equalsIgnoreCase(bugId))
        .findFirst()
        .get()
        .getString("commit");
  }

  public static String mapBugToScript(String bugId, String commit, String testParams,
      String level) {
    var cd = "cd flex-event-registration/event-registration-service-web;";
    var gitCheckout = "git checkout " + commit + ";";
    var setJdk18 = "setjdk 1.8;";
    var profile =
        "mvn clean clover:instrument clover:clover -Dmaven.test.failure.ignore=true -Dfindbugs.skip=true -Dtest="
            + testParams + ";";
    var setJdk11 = "setjdk 11;";
    var jarCmd =
        "java -jar out/artifacts/dfl.jar "
            + bugId + " " + level + ";";
    return cd + "\n"
        + gitCheckout + "\n"
        + setJdk18 + "\n"
        + profile + "\n"
        + setJdk11 + "\n"
        + jarCmd + "\n";
  }

  private static void extractTestsUptoLevel() {

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
        + "}";

    Arrays.stream(Objects.requireNonNull(Paths.get(setupDir).toFile().listFiles()))
        .map(file -> file.getName().split("\\.")[0])
        .flatMap(bugId -> {
          var allTests = FileUtils
              .readTextFile(Paths.get(setupDir, bugId + ".txt").toFile().getAbsolutePath())
              .collect(toList());

          var failedTestPosition = IntStream.range(0, allTests.size())
              .boxed()
              .filter(i -> allTests.get(i).startsWith("*"))
              .findFirst()
              .get();

          var commit = lookupCommit(bugId);

          return levels.stream().map(level -> Json.createObjectBuilder()
              .add("bugId", bugId)
              .add("level", level)
              .add("mvnCmd", mapBugToScript(bugId, commit,
                  convertTestsToMvnCmd(allTests.subList(0, failedTestPosition + level + 1)),
                  level.toString()))
              .build());
        })
        .collect(groupingBy(jsonObject -> jsonObject.getJsonNumber("level").toString(),
            Collectors.collectingAndThen(
                Collectors.mapping(jsonObject -> jsonObject.getString("mvnCmd"),
                    Collectors.joining("\n")),
                s -> scriptHeader + "\n\n" + s)))
        .entrySet()
        .forEach(e -> FileUtils.writeTextToFile(e.getValue(),
            Paths.get(levelDir, e.getKey() + ".sh").toFile().getAbsolutePath()));
  }

  public static String convertTestsToMvnCmd(List<String> tests) {
    var map = tests.stream().map(test -> test.split(":")[0])
        .map(t -> (t.startsWith("*") || t.startsWith("~")) ? t.substring(1) : t)
        .collect(groupingBy(t -> t.split("#")[0],
            Collectors.mapping(t -> t.split("#")[1], Collectors.joining("+"))));

    var allTests = map.entrySet()
        .stream()
        .map(e -> e.getKey() + "#" + e.getValue())
        .collect(joining(","));

    return allTests;
  }

  public static void extractTime(int k) {
    var bugDir = "data/1k/naturalOrder/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(bugDir).toFile().listFiles()))
        .sorted(Comparator
            .comparing(bug -> Integer.parseInt(bug.getName().split("\\.")[0].split("-")[1])))
        .forEach(bug -> {
          var testList = FileUtils.readTextFile(bug.getAbsolutePath()).collect(toList());
          var baseIndex = IntStream.range(0, testList.size())
              .filter(i -> testList.get(i).startsWith("*")).findFirst().getAsInt();
          var subTests = testList.subList(0, baseIndex + 1 + k);
          var time = subTests.stream().map(line -> Double.parseDouble(line.split(":")[1]))
              .reduce(0.0, Double::sum);
          System.out.printf("%.2f%n", time);
        });
    System.out.println("\n\n");
  }

  private static List<String> getSubTestList(String failedTest, List<String> sequence) {
    var result = sequence.stream().takeWhile(test -> !test.contains(failedTest))
        .collect(Collectors.toList());
    var failedTestFullName = sequence.stream().filter(test -> test.contains(failedTest)).findFirst()
        .get();
    result.add(failedTestFullName);
    return result;
  }

  private static String extractToMvnCmd(List<String> tests) {
    return tests.stream()
        .map(test -> test.split(":")[0])
        .collect(
            groupingBy(test -> test.split("#")[0], Collectors.mapping(test -> test.split("#")[1],
                Collectors.joining("+"))))
        .entrySet()
        .stream()
        .map(e -> e.getKey() + "#" + e.getValue())
        .collect(Collectors.joining(","));
  }

  private static Double extractTimeTaken(List<String> tests) {
    return tests.stream()
        .map(test -> test.split(":")[1])
        .map(Double::valueOf)
        .reduce(0.0, Double::sum);
  }

  public static Map.Entry<String, List<String>> extractLevelSubsetFailedTest(int level,
      String bugId) {
    var executionDir = "data/1k/execution-order";
    var sequenceOrder = FileUtils
        .readTextFile(Paths.get(executionDir, bugId, "method-order.txt").toFile().getAbsolutePath())
        .collect(toList());

    var bugMap = JsonUtils.parseJsonFromLocalFile(bugMapPath, JSON_TYPE.AS_JSON_ARRAY).orElseThrow()
        .asJsonArray();
    var bugJsonObject = JsonUtils.<JsonObject>toStream(bugMap)
        .filter(bug -> bug.getString("bugId").equalsIgnoreCase(bugId)).findFirst().orElseThrow();
    var failedTests = bugJsonObject.getJsonArray("failed_tests");

    var results = JsonUtils.<JsonValue>toStream(failedTests)
        .map(JsonValue::toString)
        .map(name -> name.replace("\"", ""))
        .map(failedTest -> getSubTestList(failedTest, sequenceOrder))
        .sorted(Comparator.comparing(List::size))
        .collect(Collectors.toList());

    return new SimpleEntry<>(bugJsonObject.getString("commit"), results.get(level));
  }

  public static String extractFailedTestSequence(int level, String bugId) {

    var entry = extractLevelSubsetFailedTest(level, bugId);

    var mvnCmd = extractToMvnCmd(entry.getValue());
    var commitId = entry.getKey();

    var cd = "cd flex-event-registration/event-registration-service-web;\n";
    var gitCheckout = "git checkout " + commitId + ";\n";
    var jdk8 = "setjdk 1.8;\n";
    var mvn =
        "mvn clean clover:instrument clover:clover -Dmaven.test.failure.ignore=true -Dfindbugs.skip=true -Denforcer.skip=true -Dtest="
            + mvnCmd + ";\n";
    var jdk11 = "setjdk 11;\n";
    var jar =
        "java -jar out/artifacts/dfl.jar " + bugId + " "
            + level + " natural;\n\n";

    return cd + gitCheckout + jdk8 + mvn + jdk11 + jar;
  }

  public static void main(String[] args) {
    cmdGen();
    createLevelResultsDir();
    timeTakenCal();
  }

  public static String extractTimeTakens(int level, String bugId) {
    var executionDir = "data/1k/execution-order";
    var sequenceOrder = FileUtils
        .readTextFile(Paths.get(executionDir, bugId, "method-order.txt").toFile().getAbsolutePath())
        .collect(toList());

    var bugMap = JsonUtils.parseJsonFromLocalFile(bugMapPath, JSON_TYPE.AS_JSON_ARRAY).get()
        .asJsonArray();
    var bugJsonObject = JsonUtils.<JsonObject>toStream(bugMap)
        .filter(bug -> bug.getString("bugId").equalsIgnoreCase(bugId)).findFirst().get();
    var failedTests = bugJsonObject.getJsonArray("failed_tests");

    var results = JsonUtils.<JsonValue>toStream(failedTests)
        .map(JsonValue::toString)
        .map(name -> name.replace("\"", ""))
        .map(failedTest -> getSubTestList(failedTest, sequenceOrder))
        .sorted(Comparator.comparing(List::size))
        .collect(Collectors.toList());

    var position = results.get(level).size() + 1;
    var timeTaken = extractTimeTaken(results.get(level));

//    return String.format("%.2f", timeTaken);
    return String.valueOf(position);

  }

  public static void cmdGen() {
    var header = "#!/usr/bin/env bash\n"
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
        + "}\n\n\n";
    IntStream.range(0, 9).forEach(level -> {
//      var content = header + Stream.of("Logic-6", "Logic-15", "Logic-17", "Logic-18", "Logic-19")
      var content = header + Stream.of("Logic-17")
          .map(bugId -> extractFailedTestSequence(level, bugId))
          .collect(Collectors.joining("\n"));
      FileUtils.writeTextToFile(content,
          Paths.get("data/1k/naturalOrder/levels", level + ".sh")
              .toFile().getAbsolutePath());
    });
  }

  public static void timeTakenCal() {
    IntStream.range(0, 9).forEach(level -> {
      var timeTaken = Stream.of("Logic-6", "Logic-15", "Logic-17", "Logic-18", "Logic-19")
          .peek(System.out::println)
          .map(bugId -> extractTimeTakens(level, bugId))
          .collect(Collectors.joining("\n"));
      FileUtils.writeTextToFile(timeTaken,
          Paths.get("data/1k/naturalOrder/timeTakens", level + ".txt")
              .toFile().getAbsolutePath());
    });

  }

  private static void createLevelResultsDir() {
    IntStream.range(0, 9)
        .forEach(level ->
        {
          try {
            Files.createDirectory(Paths
                .get("data/1k/naturalOrder/results",
                    String.valueOf(level)));
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }
}
