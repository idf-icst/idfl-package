package statistics.entity;

public class StatementEntity extends ExecutionEntity {

  public StatementEntity(TestCase test, String fullName, int count, AnalysisLevel level) {
    super(test, fullName, count);
    this.level = level;
    this.lineNumber = Integer.parseInt(fullName.substring(fullName.lastIndexOf(':') + 1));
  }

  public StatementEntity(String fqn, int lineNumber, AnalysisLevel analysisLevel) {
    this.qualifiedName = fqn;
    this.lineNumber = lineNumber;
    this.name = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    this.level = analysisLevel;
  }
}
