package statistics.entity;

public enum AnalysisLevel {
  STATEMENT("statement"),
  METHOD("method"),
  BRANCH("branch");

  String levelName;

  AnalysisLevel(String level) {
    levelName = level;
  }
}
