package statistics.entity;

import java.util.Collection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@Setter
@Getter
public class TestCase {

  private String id = "";
  private String name;
  private String qualifyName;
  private boolean passed;
  private TestStatus status;
  private Set<AbstractEntity> entities = new HashSet<>();
  private Program parentProgram;
  private LocalDateTime started = LocalDateTime.MAX;
  private LocalDateTime ended = LocalDateTime.MAX;

  public boolean isEmpty() {
    return id.equals("");
  }

  public void copyFrom(TestCase otherTest) {
    this.id = otherTest.id;
    this.name = otherTest.name;
    this.qualifyName = otherTest.qualifyName;
    this.passed = otherTest.passed;
    this.status = otherTest.status;
    this.entities = otherTest.entities;
    this.parentProgram = otherTest.parentProgram;
    this.started = otherTest.started;
    this.ended = otherTest.ended;
  }

  @Override
  public boolean equals(Object anotherTestCase) {
    if (anotherTestCase.getClass() == TestCase.class) {
      var test = ((TestCase) anotherTestCase);
      return this.id.equals(test.getId()) && this.name.equals(test.getName()) && (
          this.qualifyName == null || this.qualifyName.equals(test.getQualifyName()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((qualifyName == null) ? 0 : qualifyName.hashCode());
    result = prime * result + (name == null ? 0 : name.hashCode());
    result = prime * result + (id == null ? 0 : id.hashCode());
    return result;
  }

  public TestCase(String formattedLine) {
    qualifyName = formattedLine.substring(0, formattedLine.lastIndexOf('-'));
    name = qualifyName.substring(qualifyName.lastIndexOf('.') + 1);
    passed = Boolean.valueOf(formattedLine.substring(formattedLine.lastIndexOf('-') + 1));
    status = passed ? TestStatus.PASSED : TestStatus.FAILED;
  }

    @SuppressWarnings("unchecked")
    public TestCase(Object object) {
        JSONObject jsonObject = (JSONObject) object;
        qualifyName = jsonObject.get("test_name").toString();
        name = qualifyName.substring(qualifyName.lastIndexOf('.')+1);
        passed = Boolean.valueOf(jsonObject.get("status").toString());
        status = TestStatus.valueOf(jsonObject.get("status").toString());
        JSONArray entityArray = (JSONArray) jsonObject.get("covered_elements");
        entities.addAll((Collection<? extends AbstractEntity>) entityArray.stream()
                .map(e -> ExecutionEntity.createEntity(this, (JSONObject) e))
                .collect(Collectors.toList()));
    }

  public TestCase(JsonObject jsonObject) {
    name = jsonObject.getString("name");
    id = jsonObject.getString("id");
    status = TestStatus.valueOf(jsonObject.getString("status").toUpperCase());
    passed = jsonObject.getBoolean("passed");

    var locationFQN = jsonObject.getString("FQN");

    entities.addAll(jsonObject.getJsonArray("lines")
        .getValuesAs(jsonValue -> Integer.parseInt(jsonValue.toString()))
        .stream()
        .map(line -> ExecutionEntity.createEntity(locationFQN, line, AnalysisLevel.STATEMENT))
        .collect(Collectors.toSet())); // added STATEMENT entities

    entities.addAll(jsonObject.getJsonArray("methods")
        .getValuesAs(jsonValue -> Integer.parseInt(jsonValue.toString()))
        .stream()
        .map(line -> ExecutionEntity.createEntity(locationFQN, line, AnalysisLevel.METHOD))
        .collect(Collectors.toSet())); // added Method entities
  }

  public Set<AbstractEntity> getEntities() {
    return entities;
  }

  public void addCoveredEntity(AbstractEntity entity) {
      if (entities == null) {
          entities = new HashSet<>();
      }
    entities.add(entity);
  }

  public String getQualifyName() {
    return qualifyName;
  }

  public boolean isPassed() {
    return passed;
  }

  public String getName() {
    return name;
  }

  public void print() {
    System.out.println("# [Test] " + qualifyName + " " + (passed ? "[PASSED]" : "[FAILED] *"));
    if (entities == null) {
      System.out.println("!!! No entity is covered by this test\n");
      return;
    }

    entities.stream()
        .filter(Objects::nonNull)
        .forEach(e -> System.out.printf("=> [%s] %s : %d\n", e.getType(), e.getQualifiedName(),
            e.getExecutionCountByTest(this)));
    System.out.println();
  }

  public void setParentProgram(Program program) {
    parentProgram = program;
  }

  public Program getParentProgram() {
    return parentProgram;
  }

  public boolean isCoveringEntityType(Class<? extends AbstractEntity> type) {
    return entities.stream().anyMatch(e -> e.getClass() == type);
  }

  public TestStatus getStatus() {
    return status;
  }

  public LocalDateTime getStartedDateTime() {
    return started;
  }

  public LocalDateTime getEndedDateTime() {
    return ended;
  }

  @Override
  public String toString() {
    return (qualifyName == null ? "" : qualifyName + ":") + name + ":" + status.toString();
  }

  public TestCase combine(TestCase otherTest) {
    if (this.isEmpty()) {
      this.copyFrom(otherTest);
      return this;
    }

    if (this == otherTest) {
      this.entities.addAll(otherTest.entities);
    }

    return this;
  }

  public static TestCase merge(TestCase t1, TestCase t2) {
    t1.entities.addAll(t2.entities);
    return t1;
  }
}
