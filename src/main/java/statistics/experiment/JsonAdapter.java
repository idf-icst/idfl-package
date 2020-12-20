package statistics.experiment;

import statistics.localization.SpectrumBasedLocalizer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class JsonAdapter {

  public static JsonObject writeSpectrumBasedLocalizerToJson(
      SpectrumBasedLocalizer localizingResult) {
    return Json.createObjectBuilder()
        .add("time_stamp", localizingResult.getTimeStamp().format(DateTimeFormatter.ISO_DATE_TIME))
        .add("program_name", localizingResult.getProgramName())
        .add("code_base", localizingResult.getProgram().getCodeBase())
        .add("code_version", localizingResult.getProgram().getVersion())
        .add("analysis_level", localizingResult.getLocalizingLevel().toString())
        .add("ranking_algorithm", localizingResult.getLocalizingAlgorithm().toString())
        .add("top_k", localizingResult.getTopK())
        .add("entities", localizingResult.getResults()
            .stream()
            .map(e -> Json.createObjectBuilder()
                .add("entity_name", e.toString())
                .add("score",
                    e.getRankingScoreByAlgorithm(localizingResult.getLocalizingAlgorithm())
                        .toString()))
            .collect((Supplier<JsonArrayBuilder>) Json::createArrayBuilder,
                JsonArrayBuilder::add,
                JsonArrayBuilder::add))
        .build();
  }

}
