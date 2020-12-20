package statistics.validate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.json.JsonValue;
import statistics.experiment.FileUtils;
import statistics.experiment.JsonUtils;
import statistics.experiment.JsonUtils.JSON_TYPE;

public class FailedTestExtractor {

  public static void main(String[] args) throws Exception {
    inspectLogic17();
  }

  private static void inspectLogic17() throws Exception {
    var sl1 = new ArrayList<>(JsonUtils
        .parseJsonFromLocalFile("data/1k.bak/bug/map/bug-map.json", JSON_TYPE.AS_JSON_ARRAY)
        .orElseThrow()
        .asJsonArray()
        .getValuesAs(JsonValue::asJsonObject)
        .stream()
        .filter(jsonObject -> jsonObject.getString("bugId").equalsIgnoreCase("Logic-17"))
        .findFirst()
        .map(jsonObject -> jsonObject.getJsonArray("failed_tests"))
        .orElseThrow()
        .getValuesAs(jsonValue -> jsonValue.toString().replace("\"", ""))); // failed tests (short list)

    var ll2 = FileUtils.readTextFile("data/1k/execution-order/Logic-17/method-order.txt")
        .collect(Collectors.toList()); //all tests executed in natural oder (long list)

    FileUtils.writeTextToFile(sl1.stream()
            .map(testName -> testName + ":" + String.valueOf(IntStream.range(0, ll2.size())
                .filter(index -> ll2.get(index).contains(testName)).findFirst().orElseThrow() + 1))
            .collect(Collectors.joining("\n")),
        "src/main/java/statistics/validate/Logic-17.failed-tests.txt");

    Thread.sleep(1000);

    FileUtils.writeTextToFile(
        FileUtils.readTextFile("src/main/java/statistics/validate/Logic-17.failed-tests.txt")
            .sorted(Comparator.comparingInt(value -> Integer.parseInt(value.split(":")[1])))
            .collect(Collectors.joining("\n")),
        "src/main/java/statistics/validate/Logic-17.failed-tests.sorted.txt");
  }

}
