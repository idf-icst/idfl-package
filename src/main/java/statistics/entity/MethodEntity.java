package statistics.entity;

public class MethodEntity extends ExecutionEntity {

  public MethodEntity(TestCase test, String fullName, int count, AnalysisLevel level) {
    super(test, fullName, count);
    this.level = level;
  }

  public MethodEntity(String fqn, int lineNumber, AnalysisLevel analysisLevel) {
    this.qualifiedName = fqn;
    this.lineNumber = lineNumber;
    this.name = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    this.level = analysisLevel;
  }
}
