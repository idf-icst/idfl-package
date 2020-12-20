package statistics.localization;

import statistics.experiment.JsonUtils;

import javax.json.*;

public class RankingAlgorithmEvaluation {

  private static String file = "A-002.json";
  private static String filePath = "src/test/resources/eval/bug_metadata/meta_" + file;
  private static JsonObject allInfo = JsonUtils.parseJsonFromLocalFile(filePath)
      .map(JsonValue::asJsonObject).orElse(JsonValue.EMPTY_JSON_OBJECT).getJsonObject("dfl-bk1");
  private static String bugLocationRaw = allInfo.getJsonObject("bug-info")
      .getJsonArray("bug-locations").get(0).toString();
  private static String bugLocation = bugLocationRaw.substring(1, bugLocationRaw.length() - 1);
  public static JsonBuilderFactory factory = Json.createBuilderFactory(null);

  private static int algorithmRanking(JsonObject singleEval) {
    JsonArray entities = singleEval.getJsonArray("entities");
    int ranking = 0;
    double score;
    double prevScore = -1.0;
    boolean found = false;
    String entityName;

    for (JsonValue prediction : entities) {
      score = Double.parseDouble(prediction.asJsonObject().getString("score"));
      entityName = prediction.asJsonObject().getString("entity_name");
      if (score != prevScore) {
        ranking++;
        prevScore = score;
      }
      if (entityName.equals(bugLocation)) {
        found = true;
        return ranking;
      }
    }

    return (found) ? ranking : 0;
  }

  public static JsonObject rankingsObjectBuilder(JsonObject singleBugInfo) {
    JsonObjectBuilder rankingsBuilder = factory.createObjectBuilder();
    for (int i = 0; i < 25; i++) {
      JsonObject singleEval = singleBugInfo.getJsonArray("evals").asJsonArray().get(i)
          .asJsonObject();
      int ranking = algorithmRanking(singleEval);
      rankingsBuilder.add(singleEval.getString("ranking_algorithm"), ranking);
    }
    JsonObject rankings = rankingsBuilder.build();
    return rankings;
  }

  public static JsonObject bugObjectBuilder() {
    String description = allInfo.getJsonObject("bug-info").getString("rationale");
    JsonObject rankings = rankingsObjectBuilder(allInfo);
    JsonObject bugObject = factory.createObjectBuilder()
        .add("file", file)
        .add("description", description)
        .add("location", bugLocation)
        .add("rankings", rankings)
        .build();
    return bugObject;
  }

  public static void main(String[] args) {
    System.out.println(bugObjectBuilder());
  }
}
