package statistics.entity;

import statistics.algorithm.RankingAlgorithm;
import statistics.algorithm.Spectrum;

import java.util.Set;

/**
 * Define common actions related to collecting coverage information
 */
public interface AbstractEntity {

  Set<TestCase> getAllTest();

  Set<TestCase> getFailedTests();

  Set<TestCase> getPassedTests();

  int getTotalNumberOfFailedTests();

  int getTotalNumberOfPassedTests();

  int getTotalNumberOfTests();

  String getName();

  String getQualifiedName();

  boolean isExecutedByTest(TestCase test);

  int getExecutionCountByTest(TestCase test);

  void addTest(TestCase test, int count);

  void print();

  AnalysisLevel getType();

  Program getParentProgram();

  void setParentProgram(Program program);

  void rankAll();

  void rankByAlgorithm(RankingAlgorithm algorithm);

  Spectrum toSpectrum();

  Double getRankingScoreByAlgorithm(RankingAlgorithm algorithm);
}
