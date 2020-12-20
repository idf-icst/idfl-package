package statistics.entity;

public class BranchEntity extends ExecutionEntity {

  public BranchEntity(TestCase test, String fullName, int count, AnalysisLevel level) {
    super(test, fullName, count);
    this.level = level;
  }

  public BranchEntity(String fqn, int lineNumber, AnalysisLevel analysisLevel) {
    this.qualifiedName = fqn;
    this.lineNumber = lineNumber;
    this.name = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    this.level = analysisLevel;
  }
}
