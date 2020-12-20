package statistics.experiment;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import statistics.experiment.JsonUtils.JSON_TYPE;

import static java.util.stream.Collectors.*;

public class ExpUtils {

  public static void markFailedTestsForSingleBug(String bugDir) {
    var failedTests = JsonUtils
        .parseJsonFromLocalFile(Paths.get(bugDir, "base-setup.json").toFile().getAbsolutePath(),
            JsonUtils.JSON_TYPE.AS_JSON_OBJECT)
        .orElseThrow()
        .asJsonObject()
        .getJsonArray("failed");

    Predicate<String> isFailedTest = test -> failedTests
        .getValuesAs(jsonValue -> FileUtils.stripDoubleQuotes(jsonValue.toString()))
        .stream()
        .anyMatch(test::startsWith);

    Arrays.stream(Objects.requireNonNull(
        Paths.get(bugDir).toFile().listFiles(file -> file.getName().endsWith("000"))))
        .forEach(file -> {
          var newContent = FileUtils.readTextFile(file.getAbsolutePath())
              .map(line -> isFailedTest.test(line) ? "*" + line : line)
              .collect(Collectors.joining("\n"));
          FileUtils.writeTextToFile(newContent, Paths.get(file.getParentFile().getAbsolutePath(),
              file.getName() + "_marked_w_failed_tests").toFile().getAbsolutePath());
        });
  }

  public static void markFailedTestsForAll() {
    var rootDir = "main/resources/tests/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(rootDir).toFile().listFiles()))
        .sorted(Comparator.comparing(file -> Integer.valueOf(file.getName().split("-")[1])))
        .forEach(file -> markFailedTestsForSingleBug(file.getAbsolutePath()));
  }

  public static String extractMvnCmd(List<String> tests) {
    var map = tests.stream().map(test -> test.split(":")[0])
        .map(t -> (t.startsWith("*") || t.startsWith("~")) ? t.substring(1) : t)
        .collect(groupingBy(t -> t.split("#")[0],
            Collectors.mapping(t -> t.split("#")[1], Collectors.joining("+"))));

    var allTests = map.entrySet()
        .stream()
        .map(e -> e.getKey() + "#" + e.getValue())
        .collect(joining(","));

    var actualTests = map.values().stream().map(s -> s.split("\\+").length)
        .reduce(0, Integer::sum);

    return tests.size() + "->" + actualTests + "->" + allTests;
  }

  public static List<String> reduce(String markedFilePath) {
    var tests = FileUtils.readTextFile(Paths.get(markedFilePath).toFile().getAbsolutePath())
        .collect(toList());
    return IntStream.range(0, tests.size())
        .filter(
            i -> (tests.get(i).startsWith("*") || tests.get(i).startsWith("~")) || i == 4 || i == 9
                || i == 49 || i == 99 || i == 499 || i == 999)
        .peek(System.out::println)
        .boxed()
        .map(i -> tests.subList(0, i + 1))
        .map(ExpUtils::extractMvnCmd)
        .collect(Collectors.toList());
  }

  public static void reduceToMvnCmdForAll() {
    var rootDir = "src/main/resources/tests/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(rootDir).toFile().listFiles()))
        .sorted(Comparator.comparing(file -> Integer.valueOf(file.getName().split("-")[1])))
        .forEach(bugId -> {
          System.out.println(bugId.getName());
          Arrays.stream(
              Objects.requireNonNull(bugId
                  .listFiles(file -> file.getName().endsWith("BEST_10000_marked_w_failed_tests"))))
              .forEach(f -> {
                FileUtils.writeTextToFile(reduce(f.getAbsolutePath()).stream()
                        .peek(System.out::println)
                        .collect(Collectors.joining("\n")),
                    Paths.get(bugId.getAbsolutePath(), f.getName() + "_mvn_cmd").toFile()
                        .getAbsolutePath());
              });
        });

  }

  public static void extractRunTime() {
    var sourceDir = "src/main/resources/tests/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(sourceDir).toFile().listFiles()))
        .sorted(Comparator.comparing(file -> Integer.valueOf(file.getName().split("-")[1])))
        .forEach(file -> {
          var bugIdFile = Objects
              .requireNonNull(
                  file.listFiles(f -> f.getName().equalsIgnoreCase("BEST_10000_time")))[0];
          System.out.println(file.getName());
          System.out.println(FileUtils.readTextFile(bugIdFile.getAbsolutePath())
              .filter(line ->
                  line.startsWith("2:")
                      || line.startsWith("3:")
                      || line.startsWith("4:")
                      || line.startsWith("6:")
                      || line.startsWith("7:")
                      || line.startsWith("8:")
                      || line.startsWith("9:")
              )
              .map(line -> line.split(":")[1])
              .collect(Collectors.joining("\n")) + "\n");
        });
  }

  public static void main(String[] args) {
//    markFailedTestsForAll();
    reduceToMvnCmdForAll();
//    constructMvnCmds();
//    extractRunTime();
//    mapBugIdToMvnCmd("data/1k/random");
//    genScript();
//    updateFailedTestsForBugMap();
//    validate();
//    genRunningScript();
//    extractTopTestsAndFails();
//    genCmdScriptsAtDiffLevels();
//    genRunningScript();
//    extractTestRunNum();
//    extractDiff();
  }

  public static void constructMvnCmds() {
    var dirRoot = "/Users/t.dao/Desktop/code/dfl-latest/defect-localization/analyzer/src/main/resources/tests/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(dirRoot).toFile().listFiles()))
        .forEach(bugIdFile -> {
          var srcFile = Arrays.stream(Objects.requireNonNull(bugIdFile.listFiles(
              file -> file.getName().equalsIgnoreCase("BEST_10000_marked_w_failed_tests_mvn_cmd"))))
              .findFirst().get();
          var scriptContent = FileUtils.readTextFile(srcFile.getAbsolutePath())
              .map(s -> s.split("->")[2])
              .map(s ->
                  "mvn clean clover:instrument clover:clover -Dmaven.test.failure.ignore=true -Dfindbugs.skip=true -Dtest="
                      + s + ";\n")
              .map(cmd -> cmd
                  + "java -jar /Users/t.dao/Desktop/code/dfl-latest/defect-localization/out/artifacts/analyzer_jar/analyzer.jar "
                  + bugIdFile.getName().split("-")[1] + ";")
              .collect(Collectors.joining("\n"));
          FileUtils.writeTextToFile(scriptContent,
              Paths.get(bugIdFile.getAbsolutePath(), "run.sh").toFile().getAbsolutePath());
        });
  }

  public static Map<String, List<String>> genCmdScriptsAtDiffLevels() {
    var setupDir = "data/1k/naturalOrder/setups";
    return Arrays.stream(Objects.requireNonNull(Paths.get(setupDir).toFile().listFiles()))
        .collect(toMap(bugIdFile -> bugIdFile.getName().split("\\.")[0], bugIdFile -> {
          var testList = FileUtils.readTextFile(bugIdFile.getAbsolutePath())
              .collect(Collectors.toList());
          var firstPosition = IntStream.range(0, testList.size()).boxed()
              .filter(l -> testList.get(l).startsWith("*")).findFirst().get();
          return IntStream.range(1, 12).boxed().map(i -> getCmdString(
              testList.subList(0, firstPosition + i).stream()
                  .map(line -> line.startsWith("*") ? line.substring(1) : line)))
              .collect(Collectors.toList());
        }));
  }


  public static String constructMvnCmds2() {
    var baseTestSetupFile = "data/1k/naturalOrder/base-tests-with-time.txt";
    return getCmdString(FileUtils.readTextFile(baseTestSetupFile));
  }

  public static String getCmdString(Stream<String> tests) {
    return tests
        .map(line -> line.split(":")[0])
        .collect(groupingBy(line -> line.split("#")[0],
            Collectors.mapping(line -> line.split("#")[1], Collectors.toSet())))
        .entrySet().stream()
        .map(e -> e.getKey() + "#" + String.join("+", e.getValue()))
        .collect(Collectors.joining(","));

  }

  private static void extractTopTestsAndFails() {
    var bugMapFile = "data/1k/naturalOrder/bug-map.json";
    var baseTestsFile = "data/1k/naturalOrder/base-tests-with-time.txt";
    var testLines = FileUtils.readTextFile(baseTestsFile).collect(toList());
    var setupDir = "data/1k/naturalOrder/setups";

    JsonUtils.parseJsonFromLocalFile(bugMapFile, JSON_TYPE.AS_JSON_ARRAY).get().asJsonArray()
        .getValuesAs(JsonValue::asJsonObject)
        .forEach(bug -> {
          var failedTests = new HashSet<>(
              bug.getJsonArray("failed_tests")
                  .getValuesAs(jsonValue -> FileUtils.stripDoubleQuotes(jsonValue.toString())));
          var content = testLines.stream()
              .map(line -> failedTests.contains(line.split(":")[0].split("#")[1])
                  ? "*" + line : line)
              .collect(Collectors.joining("\n"));
          FileUtils.writeTextToFile(content, setupDir + "/" + bug.getString("bugId") + ".txt");
        });
  }

  public static String mapBugToScript(JsonObject bugConfigDoc, String testParams) {
    var bugId = bugConfigDoc.getString("bugId");
    var commit = bugConfigDoc.getString("commit");
    var cd = "cd flex-event-registration/event-registration-service-web;";
    var gitCheckout = "git checkout " + commit + ";";
    var setJdk18 = "setjdk 1.8;";
    var profile =
        "mvn clean clover:instrument clover:clover -Dmaven.test.failure.ignore=true -Dfindbugs.skip=true -Dtest="
            + testParams + ";";
    var setJdk11 = "setjdk 11;";
    var jarCmd =
        "java -jar out/artifacts/dfl.jar "
            + bugId + ";";
    return cd + "\n"
        + gitCheckout + "\n"
        + setJdk18 + "\n"
        + profile + "\n"
        + setJdk11 + "\n"
        + jarCmd + "\n";
  }

  public static void genRunningScript() {
    var bugMapFile = "data/1k/naturalOrder/bug-map.json";
    var cmdStringMap = genCmdScriptsAtDiffLevels();

    IntStream.range(0, 11).boxed()
        .forEach(level -> {
          var scripContent = JsonUtils.parseJsonFromLocalFile(bugMapFile, JSON_TYPE.AS_JSON_ARRAY)
              .get().asJsonArray()
              .getValuesAs(JsonValue::asJsonObject)
              .stream()
              .map(jsonObject -> {
                var bugId = jsonObject.getString("bugId");
                return mapBugToScript(jsonObject, cmdStringMap.get(bugId).get(level));
              })
              .collect(Collectors.joining("\n"));

          FileUtils.writeTextToFile(scripContent,
              "data/1k/naturalOrder/levels/" + level + ".sh");
        });
  }


  public static String extractKFirstTests(String path, int k) {
    return
        "mvn clean clover:instrument clover:clover -Dmaven.test.failure.ignore=true -Dfindbugs.skip=true -Dtest="
            +
            FileUtils.readTextFile(path).limit(k)
                .map(line -> line.split(":")[0])
                .distinct()
                .collect(Collectors.collectingAndThen(groupingBy(test -> test.split("#")[0],
                    Collectors.mapping(t -> t.split("#")[1], Collectors.joining("+"))),
                    stringOptionalMap -> stringOptionalMap.entrySet().stream()
                        .map(e -> e.getKey() + "#" + e.getValue())
                        .collect(Collectors.joining(","))));
  }

  public static void mapBugIdToMvnCmd(String destDir) {
    var baseDir = "/Users/t.dao/Desktop/code/dfl-latest/defect-localization/analyzer/src/main/exp-data/tests/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(baseDir).toFile().listFiles()))
        .sorted(Comparator
            .comparing(file -> Integer.valueOf(file.getName().split("\\.")[0].split("-")[1])))
        .forEach(file -> {
          var sampleSize = 10_000;
          var k = new Random().nextInt(sampleSize);
          var bugId = file.getName();
          System.out.println(bugId);
          var commit = getCommit(file.getName());
          var cmd = extractKFirstTests(
              Paths.get(file.getAbsolutePath(), "BEST_10000").toFile().getAbsolutePath(), k);
          var time = FileUtils.readTextFile(
              Paths.get(file.getAbsolutePath(), "BEST_10000_time").toFile().getAbsolutePath())
              .filter(line -> line.startsWith(k + ":"))
              .findFirst()
              .orElseThrow();
          FileUtils.writeTextToFile(bugId + "\n" + time + "\n" + cmd + "\n" + commit,
              Paths.get(destDir, bugId + ".txt").toFile().getAbsolutePath());
        });
  }

  public static String getCommit(String bugId) {
    var bugDir = "data/bugs";
    return JsonUtils
        .parseJsonFromLocalFile(Paths.get(bugDir, bugId + ".json").toFile().getAbsolutePath(),
            JsonUtils.JSON_TYPE.AS_JSON_OBJECT).get()
        .asJsonObject().getString("commit");
  }

  public static void genScript() {
    var expSetupDir = "data/1k/random";
    var shFile = "data/1k/run-all.sh";
    FileUtils.writeTextToFile(Arrays.stream(
        Objects.requireNonNull(Paths.get(expSetupDir).toFile().listFiles()))
        .sorted(Comparator
            .comparing(file -> Integer.valueOf(file.getName().split("\\.")[0].split("-")[1])))
        .map(file -> {
          var lines = FileUtils.readTextFile(file.getAbsolutePath()).collect(toList());
          var bugId = lines.get(0).trim();
          var mvnCmd = lines.get(2);
          var commit = lines.get(3);

          return
              "cd flex-event-registration/event-registration-service-web;\n"
                  + "git checkout " + commit + ";\n"
                  + "setjdk 1.8;\n"
                  + mvnCmd + ";\n"
                  + "setjdk 11;\n"
                  + "java -jar out/artifacts/dfl.jar "
                  + bugId + ";\n";
        })
        .collect(Collectors.joining("\n")), shFile);
  }

  public static void updateFailedTestsForBugMap() {
    var bugDir = "data/bugs";
    var testDir = "data/1k/results";

    var bugIdToFailedTests = Arrays.stream(
        Objects.requireNonNull(Paths.get(testDir).toFile().listFiles()))
        .collect(Collectors.toMap(testResult -> testResult.getName().split("::")[2], testResult -> {
          return JsonUtils
              .parseJsonFromLocalFile(testResult.getAbsolutePath(), JSON_TYPE.AS_JSON_OBJECT).get()
              .asJsonObject().getJsonObject("profile")
              .getJsonArray("testView")
              .getValuesAs(jsonValue -> jsonValue.asJsonObject().getString("testCase")).stream()
              .filter(t -> t.endsWith("FAILED"))
              .map(testCase -> testCase.split(":")[0])
              .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);
        }));

    var jsonArray = Arrays.stream(Objects.requireNonNull(Paths.get(bugDir).toFile().listFiles()))
        .map(bugId -> {
          var id = bugId.getName().split("\\.")[0];
          var bugInfo = JsonUtils
              .parseJsonFromLocalFile(bugId.getAbsolutePath(), JSON_TYPE.AS_JSON_OBJECT).get()
              .asJsonObject();
          return Json.createObjectBuilder(bugInfo)
              .add("failed_tests", bugIdToFailedTests.get(id))
              .add("bugId", id)
              .build();
        })
        .sorted(Comparator
            .comparing(jsonObject -> Integer.valueOf(jsonObject.getString("bugId").split("-")[1])))
        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
        .build();
    JsonUtils.writeJsonObjectToFile(jsonArray, "data/1k/naturalOrder/bug-map.json");
  }

  public static void validate() {
    var targetFile = "data/1k/naturalOrder/bug-map.json";
    System.out.println(
        JsonUtils.parseJsonFromLocalFile(targetFile, JSON_TYPE.AS_JSON_ARRAY).orElseThrow()
            .asJsonArray()
            .getValuesAs(jsonValue -> jsonValue.asJsonObject().getJsonArray("failed_tests").size()
                == jsonValue.asJsonObject().getJsonNumber("fails").intValue()).stream()
            .filter(b -> !b)
            .count());
  }

  public static void extractTestRunNum() {
    var baseDir = "data/1k/naturalOrder/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(baseDir).toFile().listFiles()))
        .sorted(Comparator
            .comparing(bugId -> Integer.valueOf(bugId.getName().split("\\.")[0].split("-")[1])))
        .forEach(bugId -> {
          var lineNum = FileUtils.readTextFile(bugId.getAbsolutePath())
              .takeWhile(line -> !line.startsWith("*")).count() + 1;
          System.out.println(lineNum);
        });
  }

  public static void extractDiff() {
    var dest = ".";
    var gitCMd = Arrays
        .stream(Objects.requireNonNull(Paths.get("data/bugs").toFile().listFiles()))
        .map(bugId -> {
          var headCommit = JsonUtils.parseJsonFromLocalFile(bugId.getAbsolutePath()).get()
              .asJsonObject().getString("commit");
          var prevCommit = headCommit + "~1";
          return "git diff " + headCommit + " " + prevCommit + "> " + dest + "/diff-dir/" + bugId
              .getName().split("\\.")[0] + ".txt;";
        })
        .collect(Collectors.joining("\n"));
    var cdCmd = "cd flex-event-registration/event-registration-service-web";
    FileUtils.writeTextToFile(cdCmd + "\n" + gitCMd, dest + "/" + "extractDiff.sh");
  }
}
