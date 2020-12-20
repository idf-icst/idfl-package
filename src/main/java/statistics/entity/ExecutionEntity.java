package statistics.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.simple.JSONObject;
import statistics.algorithm.AlgorithmCollection;
import statistics.algorithm.RankingAlgorithm;
import statistics.algorithm.Spectrum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExecutionEntity implements AbstractEntity {

  String name;
  String qualifiedName;
  AnalysisLevel level;
  int lineNumber;
  Map<TestCase, Integer> executionCount = new HashMap<>();
  Program parentProgram;
  Map<RankingAlgorithm, Double> ranks = new HashMap<>();

  public static AbstractEntity createEntity(String fqn, int lineNumber,
      AnalysisLevel analysisLevel) {
    switch (analysisLevel) {
      case STATEMENT:
        return new StatementEntity(fqn, lineNumber, analysisLevel);
      case METHOD:
        return new MethodEntity(fqn, lineNumber, analysisLevel);
      case BRANCH:
        return new BranchEntity(fqn, lineNumber, analysisLevel);
      default:
        return null;
    }
  }

  public Double getRankScoreByAlgorithm(RankingAlgorithm algorithm) {
    return ranks.get(algorithm);
  }

  public ExecutionEntity(TestCase test, String fullName, int count) {
    qualifiedName = fullName;
    name = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    executionCount.put(test, count);
  }

  public static AbstractEntity createEntity(TestCase test, String fullName, int count,
      AnalysisLevel level) {
    switch (level) {
      case STATEMENT:
        return new StatementEntity(test, fullName, count, level);
      case METHOD:
        return new MethodEntity(test, fullName, count, level);
      case BRANCH:
        return new BranchEntity(test, fullName, count, level);
    }
    return null;
  }

  public static AbstractEntity createEntity(TestCase testCase, JSONObject jsonObject) {
    String entityFullName = jsonObject.get("element_name").toString();
    int count = Integer.parseInt(jsonObject.get("count").toString());
    AnalysisLevel level = AnalysisLevel.valueOf(jsonObject.get("type").toString());
    return createEntity(testCase, entityFullName, count, level);
  }

  public void addTest(TestCase test, int count) {
    executionCount.put(test, count);
  }

  @Override
  public Set<TestCase> getAllTest() {
    return executionCount.keySet();
  }

  @Override
  public Set<TestCase> getFailedTests() {
    return executionCount.keySet().stream()
        .filter(p -> !p.isPassed())
        .collect(toSet());
  }

  @Override
  public Set<TestCase> getPassedTests() {
    return executionCount.keySet().stream()
        .filter(TestCase::isPassed)
        .collect(toSet());
  }

  @Override
  public int getTotalNumberOfFailedTests() {
    return getFailedTests().size();
  }

  @Override
  public int getTotalNumberOfPassedTests() {
    return getPassedTests().size();
  }

  @Override
  public int getTotalNumberOfTests() {
    return executionCount.keySet().size();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public boolean isExecutedByTest(TestCase test) {
    return executionCount.containsKey(test);
  }

  @Override
  public int getExecutionCountByTest(TestCase test) {
    return executionCount.get(test);
  }

  public void print() {
    System.out.println("=>" + level.levelName + ": " + qualifiedName);
    executionCount
        .forEach((key, value) -> System.out.println(
            "[executed by] " + key.getQualifyName() + ": " + value + ": " + (key.isPassed()
                ? "[PASSED]" : "[FAILED] *")));
  }

  public AnalysisLevel getType() {
    return level;
  }

  @Override
  public Program getParentProgram() {
    return parentProgram;
  }

  @Override
  public void rankAll() {
    ranks = Arrays.stream(RankingAlgorithm.values())
        .collect(toMap(Function.identity(), this::rankBy));
  }

  @Override
  public Spectrum toSpectrum() {
    return Spectrum.create(this);
  }

  public Double rankBy(RankingAlgorithm algorithm) {
    return AlgorithmCollection.AlgorithmSelectorFunction.apply(toSpectrum(), algorithm);
  }

  @Override
  public void rankByAlgorithm(RankingAlgorithm algorithm) {
    ranks.put(algorithm, rankBy(algorithm));
  }

  @Override
  public Double getRankingScoreByAlgorithm(RankingAlgorithm algorithm) {
    return ranks.get(algorithm);
  }

  @Override
  public boolean equals(Object entity) {
    return this.getType().equals(((ExecutionEntity) entity).getType())
        && this.qualifiedName.equals(((ExecutionEntity) entity).getQualifiedName())
        && this.lineNumber == ((ExecutionEntity) entity).getLineNumber();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (qualifiedName + lineNumber + getType().toString()).hashCode();
    return result;
  }

  @Override
  public void setParentProgram(Program program) {
    parentProgram = program;
  }

  @Override
  public String toString() {
    return level.toString() + ":" + qualifiedName + ":" + lineNumber;
  }
}
