package statistics.localization;


import statistics.algorithm.RankingAlgorithm;
import statistics.entity.AnalysisLevel;

public enum LocalizationParams {
  DBA_REPORT("off", "If localization results are reported to DBA"),
  ALGORITHM(RankingAlgorithm.TARANTULA, "Which ranking algorithm to use"),
  LEVEL(AnalysisLevel.STATEMENT, "Which analysis level used"),
  TOPK(50, "Get only top k highest ranked elements"),
  PROJECT_DIR("", ""),
  PROJECT_PREFIX("", "");

  String paramStringValue;
  String paramDescription;
  RankingAlgorithm rankingAlgorithm;
  AnalysisLevel level;
  int topK;

  LocalizationParams(String param, String description) {
    paramStringValue = param;
    paramDescription = description;
  }

  LocalizationParams(RankingAlgorithm rankingAlgorithm, String paramDescription) {
    this.rankingAlgorithm = rankingAlgorithm;
    this.paramDescription = paramDescription;
  }

  LocalizationParams(AnalysisLevel analysisLevel, String paramDescription) {
    level = analysisLevel;
    this.paramDescription = paramDescription;
  }

  LocalizationParams(int intValue, String paramDescription) {
    topK = intValue;
    this.paramDescription = paramDescription;
  }

  RankingAlgorithm getRankingAlgorithm() {
    return rankingAlgorithm;
  }
}
