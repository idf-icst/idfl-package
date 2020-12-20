package statistics.localization;

import lombok.Getter;
import lombok.Setter;
import statistics.algorithm.RankingAlgorithm;
import statistics.entity.AbstractEntity;
import statistics.entity.AnalysisLevel;
import statistics.entity.Program;
import statistics.experiment.JsonAdapter;

import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static statistics.algorithm.RankingAlgorithm.TARANTULA;
import static statistics.entity.AnalysisLevel.STATEMENT;
import static statistics.profiling.CloverParams.TOP_K;

@Setter
@Getter
public class SpectrumBasedLocalizer {

  Program program;
  List<AbstractEntity> rankedList;
  RankingAlgorithm rankingAlgorithm = TARANTULA;
  AnalysisLevel analysisLevel = STATEMENT;
  LocalDateTime timeStamp;
  int topK = TOP_K.getIntValue();

  private SpectrumBasedLocalizer(Program program) {
    this.program = program;
  }

  public static SpectrumBasedLocalizer accept(Program program) {
    return new SpectrumBasedLocalizer(program);
  }

  public SpectrumBasedLocalizer rankByAlgorithmAtLevel(RankingAlgorithm algorithm,
      AnalysisLevel level) {
    rankedList = program.indexByAlgorithm(algorithm)
        .getEntitySet()
        .stream()
        .filter(e -> e.getType() == level)
        .collect(Collectors.toList());
    rankedList.sort((e1, e2) -> -e1.getRankingScoreByAlgorithm(algorithm)
        .compareTo(e2.getRankingScoreByAlgorithm(algorithm)));
    return this;
  }

  public List<AbstractEntity> getTopK(int k) {
    return rankedList.subList(0, Math.min(k, rankedList.size()));
  }

  public List<AbstractEntity> getResults() {
    return getTopK(topK);
  }

  private SpectrumBasedLocalizer rank() {
    return rankByAlgorithmAtLevel(rankingAlgorithm, analysisLevel);
  }

  public SpectrumBasedLocalizer setAlgorithm(RankingAlgorithm algorithm) {
    rankingAlgorithm = algorithm;
    return this;
  }

  public SpectrumBasedLocalizer setAnalysisLevel(AnalysisLevel level) {
    this.analysisLevel = level;
    return this;
  }

  public SpectrumBasedLocalizer localizeBug() {
    timeStamp = LocalDateTime.now();
    return rank();
  }

  public SpectrumBasedLocalizer setTopK(int k) {
    topK = k;
    return this;
  }

  public String getProgramName() {
    return program.getName();
  }

  public RankingAlgorithm getLocalizingAlgorithm() {
    return rankingAlgorithm;
  }

  public AnalysisLevel getLocalizingLevel() {
    return analysisLevel;
  }

  public JsonObject reportToJson() {
    return JsonAdapter.writeSpectrumBasedLocalizerToJson(this);
  }

}
