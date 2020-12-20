package statistics.experiment;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static statistics.experiment.JsonUtils.JSON_TYPE.AS_JSON_OBJECT;

public class TestCaseExtractor {

  public static void extractTests(String targetFile) {
    var xmlTestsDir = "flex-event-registration/event-registration-service-web/target/clover/surefire-reports";
    var xmls = Paths.get(xmlTestsDir).toFile()
        .listFiles();
    var tests = Arrays.stream(xmls)
        .filter(file -> file.getName().endsWith(".xml"))
        .flatMap(file -> {
          try {
            return Files.lines(file.toPath())
                .filter(s -> s.trim().startsWith("<testcase"))
                .map(s -> s.split("\""));
          } catch (IOException e) {
            e.printStackTrace();
          }
          return Stream.empty();
        })
        .map(a -> a[1] + "#" + a[3] + ":" + String
            .format("%.3f", Double.parseDouble(a[5])))
        .collect(Collectors.joining("\n"));
    FileUtils.writeTextToFile(tests, targetFile);
  }


  public static void extractTestsInOrder(String orderFile, String... targetFile) {
    var rootTestDir = "flex-event-registration/event-registration-service-web/target/clover/surefire-reports";
    var tests = FileUtils.readTextFile(orderFile)
        .map(test -> "TEST-" + test.split(" ")[1].trim() + ".xml")
        .peek(System.out::println)
        .flatMap(testFile -> FileUtils.readTextFile(rootTestDir + "/" + testFile)
            .filter(s -> s.trim().startsWith("<testcase"))
            .map(s -> s.split("\""))
            .map(a -> a[1] + "#" + a[3] + ":" + String
                .format("%.3f", Double.parseDouble(a[5]))))
        .collect(Collectors.joining("\n"));
    FileUtils.writeTextToFile(tests, targetFile[0]);
  }

  public static void extractTestsInOrder2(String orderFile, String... targetFile) {
    var rootTestDir = "flex-event-registration/event-registration-shared/target/clover/surefire-reports";
    var tests = FileUtils.readTextFile(orderFile)
        .map(test -> "TEST-" + (test.contains(" ") ? test.split(" ")[1].trim() : test) + ".xml")
        .peek(System.out::println)
        .flatMap(testFile -> FileUtils.readTextFile(rootTestDir + "/" + testFile)
            .filter(s -> s.trim().startsWith("<testcase"))
            .map(s -> s.split("\""))
            .map(a -> a[1] + "#" + a[3] + ":" + String
                .format("%.3f", Double.valueOf(a[5]) + new Random().nextDouble())))
        .collect(Collectors.joining("\n"));

    FileUtils.writeTextToFile(tests, targetFile[0]);
  }

  public static void generateTestSetup() {
    var testResultDir = "src/main/resources/results";
    var allBaseTestsFilePath = "src/main/resources/tests/base/base-tests";
    var baseTestSetupDir = "src/main/resources/tests/setups";

    var baseTests = FileUtils
        .readTextFile(allBaseTestsFilePath)
        .collect(toList());

    Arrays.stream(Objects.requireNonNull(Paths.get(testResultDir).toFile().listFiles()))
        .sorted(Comparator.comparing(file -> file.getName().split("::")[2]))
        .map(TestCaseExtractor::searchForFailedTests)
        .map(entry -> indexFailedTestCases(baseTests, entry))
        .forEach(bugIdObject -> {
          var dir = Paths
              .get(baseTestSetupDir, bugIdObject.getString("id").split("::")[2], "base-setup.json");
          if (!dir.toFile().exists()) {
            JsonUtils.writeJsonObjectToFile(bugIdObject, dir.toString());
            System.out.println(dir.toString() + " has been created.");
          }
        });
  }

  private static void createSetupIdDirs() {
    var sourceResultDir = "src/main/resources/results";
    var destDir = "src/main/resources/tests/setups";

    Arrays.stream(Paths.get(sourceResultDir).toFile().listFiles())
        .map(file -> file.getName().split("::")[2])
        .sorted(Comparator.comparing(name -> Integer.valueOf(name.split("-")[1])))
        .filter(id -> !Paths.get(destDir, id).toFile().exists())
        .forEach(id -> {
          try {
            Files.createDirectory(Paths.get(destDir, id));
            System.out.println(id + " has been created");
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }

  private static Map<String, String> calTestToTimeMap() {
    var withTimeVersionFile = "src/main/resources/tests/base/base-tests-with-time";
    return FileUtils.readTextFile(withTimeVersionFile)
        .collect(Collectors.toMap(t -> t.split(":")[0], t -> t.split(":")[1]));
  }

  private static Map<String, String> tetWithTimeIndex = Map.of();

  private static String mapToTimeVersion(String testName) {
    var time = tetWithTimeIndex.get(testName);
    return testName + ":" + time;
  }

  private static void writeTestSetToFile(JsonObject jsonObject, String orderType, int k,
      File targetFile) {
    var failedTests = jsonObject.getJsonArray("failed");
    var passedTests = jsonObject.getJsonArray("passed");

    var listFails = failedTests
        .getValuesAs(jsonValue -> FileUtils.stripDoubleQuotes(jsonValue.toString()));
    Collections.shuffle(listFails);

    var listPasses = passedTests
        .getValuesAs(jsonValue -> FileUtils.stripDoubleQuotes(jsonValue.toString()));

    if (k < (listFails.size() + listPasses.size())) {
      throw new IllegalArgumentException("Needed a larger k");
    }

    var neededPassedTestNumber = k - listFails.size();

    var genPassedList = IntStream.range(0, neededPassedTestNumber)
        .boxed()
        .map(i -> listPasses.get(i % listPasses.size()))
        .collect(Collectors.toList());
    Collections.shuffle(genPassedList);

    BiFunction<String, Integer, String> findFileName = (type, l) -> Paths
        .get(targetFile.getAbsolutePath(), type + "_" + l).toFile().getAbsolutePath();

    switch (orderType.toUpperCase()) {
      case "BEST":
        FileUtils.writeTextToFile(Stream.concat(listFails.stream(), genPassedList.stream())
            .map(TestCaseExtractor::mapToTimeVersion)
            .collect(Collectors.joining("\n")), findFileName.apply("BEST", k));
        return;
      case "WORST":
        FileUtils.writeTextToFile(Stream.concat(genPassedList.stream(), listFails.stream())
            .map(TestCaseExtractor::mapToTimeVersion)
            .collect(Collectors.joining("\n")), findFileName.apply("WORST", k));
        return;
      default:
    }
  }

  private static void generateTestSetSample() {
    var setupsDir = "src/main/resources/tests/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(setupsDir).toFile().listFiles()))
        .sorted(Comparator.comparing(file -> Integer.valueOf(file.getName().split("-")[1])))
        .forEach(file -> {
          var jsonObject = JsonUtils.parseJsonFromLocalFile(
              Paths.get(file.getAbsolutePath(), "base-setup.json").toFile().getAbsolutePath(),
              AS_JSON_OBJECT).get();
          Stream.of("BEST", "WORST", "RANDOM")
              .forEach(type -> writeTestSetToFile(jsonObject.asJsonObject(), type, 1000, file));
        });
  }

  @SuppressWarnings("unchecked")
  public static JsonObject indexFailedTestCases(List<String> baseTests,
      Map.Entry<String, List<String>> failedTestCases) {
    Predicate<String> ifBaseTestFailed = (baseTest) -> failedTestCases.getValue().stream()
        .anyMatch(failedTest -> baseTest.endsWith(failedTest.split(":")[0]));
    var partition = baseTests.stream()
        .collect(Collectors.partitioningBy(ifBaseTestFailed.negate(),
            Collector.of(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)));
    var failedTests = partition.get(false);
    var passedTests = partition.get(true);
    return Json.createObjectBuilder()
        .add("id", failedTestCases.getKey())
        .add("failed", failedTests)
        .add("passed", passedTests)
        .build();
  }

  @SuppressWarnings("unchecked")
  public static Map.Entry<String, List<String>> searchForFailedTests(File resultFile) {
    return new AbstractMap.SimpleEntry<String, List<String>>(resultFile.getName(),
        Stream.of(resultFile)
            .map(file -> JsonUtils.parseJsonFromLocalFile(file.getAbsolutePath(), AS_JSON_OBJECT))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(
                jsonValue -> jsonValue.asJsonObject().getJsonObject("profile")
                    .getJsonArray("testView"))
            .flatMap(jsonValues -> jsonValues
                .getValuesAs(jsonValue -> jsonValue.asJsonObject().getString("testCase")).stream()
                .filter(s -> s.endsWith(":FAILED")))
            .collect(toList()));
  }

  private static void reduce() {
    var baseSetupDir = "src/main/resources/tests/setups";
    Arrays.stream(Objects.requireNonNull(Paths.get(baseSetupDir).toFile().listFiles()))
        .filter(file -> file.getName().equalsIgnoreCase("Logic-10"))
        .forEach(bugId -> {
          Arrays.stream(
              Objects.requireNonNull(bugId.listFiles(file -> file.getName().endsWith("0"))))
              .forEach(fileName -> {
                var times = FileUtils.readTextFile(fileName.getAbsolutePath())
                    .map(t -> Double.valueOf(t.split(":")[1])).collect(toList());
                var timeIndex = diagonalyReduce(times);
                var content = timeIndex.entrySet().stream()
                    .map(e -> e.getKey() + ":" + String.format("%.3f", e.getValue()))
                    .collect(Collectors.joining("\n"));
                FileUtils.writeTextToFile(content,
                    Paths.get(bugId.getAbsolutePath(), fileName.getName() + "_time").toFile()
                        .getAbsolutePath());
              });
        });
  }

  private static Map<Integer, Double> diagonalyReduce(List<Double> doubles) {
    var result = new HashMap<Integer, Double>();
    result.put(1, doubles.get(0));
    IntStream.range(2, doubles.size() + 1)
        .boxed()
        .forEach(j -> result.put(j, result.get(j - 1) + doubles.get(j - 1)));
    return result;
  }
}
