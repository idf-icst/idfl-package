package statistics.experiment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import lombok.NonNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import javax.json.*;
import java.io.*;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static statistics.experiment.JsonUtils.JSON_TYPE.AS_JSON_OBJECT;

public class JsonUtils {

  public enum JSON_TYPE {
    AS_JSON_OBJECT,
    AS_JSON_ARRAY
  }

  public static JSONObject readJsonFromFile(String pathToJsonFile) {
    JSONParser jsonParser = new JSONParser();
    JSONObject jsonObject = null;
    try {
      jsonObject = (JSONObject) jsonParser.parse(new FileReader(pathToJsonFile));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return jsonObject;
  }

  public static void
  writeJsonObjectToFile(JsonValue jsonValue, String fileName) {
    try (OutputStream os = new FileOutputStream(fileName); JsonWriter jsonWriter = Json
        .createWriterFactory(null).createWriter(os)) {
      switch (jsonValue.getValueType()) {
        case OBJECT:
          jsonWriter.writeObject(jsonValue.asJsonObject());
          return;
        case ARRAY:
          jsonWriter.writeArray(jsonValue.asJsonArray());
          return;
        default:
          throw new UnsupportedOperationException("Not supported");
      }
    } catch (IOException e) {
      System.out.println("Check if file path is correct.");
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
    }
  }

  public static JsonObject readJsonObjectToFile(String fileName) {
    JsonReaderFactory factory = Json.createReaderFactory(null);
    try (InputStream inputStream = new FileInputStream(fileName); JsonReader jsonReader = factory
        .createReader(inputStream)) {
      return jsonReader.read().asJsonObject();
    } catch (IOException e) {
      System.out.println("Check if file path is correct");
    }
    return JsonValue.EMPTY_JSON_OBJECT;
  }

  public static String jsonFormatter(String jsonString) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    com.google.gson.JsonParser jp = new com.google.gson.JsonParser();
    JsonElement je = jp.parse(jsonString);
    return gson.toJson(je);
  }

  public static Optional<? extends JsonValue> parseJsonFromDataStream(InputStream inputStream,
      JSON_TYPE... type) {
    try (javax.json.stream.JsonParser jp = Json.createParser(inputStream)) {
      return parseJsonFromDataStream(jp, type);
    } catch (Exception jsonException) {
    }
    return Optional.empty();
  }

  public static Optional<? extends JsonValue> parseJsonFromDataStream(
      javax.json.stream.JsonParser jp, JSON_TYPE... type) {
    try {
        if (!jp.hasNext()) {
            return Optional.empty();
        }
      jp.next();
      return Optional
          .of((type == null || type.length < 1 || type[0] == AS_JSON_OBJECT) ? jp.getObject()
              : jp.getArray());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  public static Optional<? extends JsonValue> parseJsonFromLocalFile(String filePath,
      JSON_TYPE... type) {
    try {
      return parseJsonFromDataStream(new FileInputStream(new File(filePath)), type);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  public static Stream<JsonObject> flattenJsonValue(JsonValue jsonValue) {
      if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {
          return Stream.of(jsonValue.asJsonObject());
      } else {
          return jsonValue.asJsonArray().stream().flatMap(JsonUtils::flattenJsonValue);
      }
  }

  public static <T> Stream<T> toStream(@NonNull JsonArray source) {
    return IntStream.range(0, source.size()).boxed().map(source::get).map(o -> (T) o);
  }
}
