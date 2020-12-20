package statistics.entity;

import lombok.Getter;
import lombok.Setter;
import statistics.algorithm.RankingAlgorithm;
import statistics.algorithm.SpectrumBasedIndexing;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Setter
public class Program implements SpectrumBasedIndexing {

  private Set<AbstractEntity> entitySet;
  private Set<TestCase>
      testSet;
  private String name = "";
  private String version = "";
  private LocalDateTime localDateTime;
  private String codeBase = "";

  public Program(Set<AbstractEntity> entities, Set<TestCase> tests) {
    entitySet = entities;
    testSet = tests;
    entitySet.forEach(e -> e.setParentProgram(this));
    testSet.forEach(t -> t.setParentProgram(this));
    localDateTime = LocalDateTime.now();
  }

  public Program(Set<TestCase> tests) {
    testSet = tests;
    entitySet = testSet.stream()
        .map(TestCase::getEntities)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  public static Program createProgramFromEntitySet(Set<AbstractEntity> entitySet) {
    Set<TestCase> tests = entitySet.stream()
        .map(e -> ((ExecutionEntity) e).getExecutionCount().keySet())
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
    return new Program(entitySet, tests);
  }

  public Set<TestCase> getTestCases() {
    return testSet;
  }

  public Program() {
    entitySet = new HashSet<>();
    testSet = new HashSet<>();
  }

  /* to be corrected */
  public Program addProgram(Program anotherProgram) {
      if (entitySet.isEmpty() && testSet.isEmpty()) {
          return anotherProgram;
      }

    this.testSet.stream().forEach(test ->
        anotherProgram.getTestCases()
            .stream()
            .filter(t -> t.equals(test))
            .findFirst()
            .ifPresent(foundTest -> test.getEntities().addAll(foundTest.getEntities()))
    );
    this.getEntitySet().addAll(anotherProgram.getEntitySet());
    return this;
  }


  public static Program create(Set<TestCase> tests) {
    return new Program(tests);
  }

  public Set<AbstractEntity> getEntitySet() {
    return entitySet;
  }


  public long getTotalFailedTests(Class<? extends AbstractEntity> entityType) {
    return testSet.stream().filter(t -> !t.isPassed() && t.isCoveringEntityType(entityType))
        .count();
  }

  public long getTotalPassedTests(Class<? extends AbstractEntity> entityType) {
    return testSet.stream().filter(t -> t.isPassed() && t.isCoveringEntityType(entityType)).count();
  }

  /**
   * Index all entity will all available algorithms
   */
  public Program indexAll() {
    entitySet.forEach(AbstractEntity::rankAll);
    return this;
  }

  @Override
  public Program indexByAlgorithm(RankingAlgorithm algorithm) {
    entitySet.forEach(e -> e.rankByAlgorithm(algorithm));
    localDateTime = LocalDateTime.now();
    return this;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public void printEntitySet() {
    AtomicInteger entityIndex = new AtomicInteger();
    entityIndex.set(1);
    entitySet.forEach(e -> {
      System.out.println(entityIndex.getAndIncrement() + ". => " + e + ":");
      AtomicInteger testIndex = new AtomicInteger();
      testIndex.set(1);
      ((ExecutionEntity) e).getExecutionCount().keySet()
          .forEach(t -> System.out.println("\t" + testIndex.getAndIncrement() + ". => " + t));
    });
  }

  public void printSetTest() {
    AtomicInteger testIndex = new AtomicInteger();
    testIndex.set(1);
    testSet.forEach(testCase -> {
      System.out.println(testIndex.getAndIncrement() + ". => " + testCase);
      AtomicInteger entityIndex = new AtomicInteger();
      entityIndex.set(1);
      testCase.getEntities()
          .forEach(entity -> System.out
              .println("\t" + entityIndex.getAndIncrement() + ". => " + entity));
    });
  }

  public Program setCodeBase(String codeBase) {
    this.codeBase = codeBase;
    return this;
  }

  public Program setCodeVersion(String codeVersion) {
    this.version = codeVersion;
    return this;
  }

  public JsonObject toProfile() {

    var testView = testSet.stream()
        .map(testCase -> Json.createObjectBuilder()
            .add("testCase", testCase.toString())
            .add("entities", Json.createArrayBuilder(testCase.getEntities()
                .stream()
                .map(Object::toString)
                .sorted(Comparator.comparing(o -> Integer.valueOf(o.split(":")[2])))
                .collect(Collectors.toList()))))
        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::addAll);

    var entityView = entitySet.stream()
        .map(e -> Json.createObjectBuilder()
            .add("entity", e.toString())
            .add("tests", Json.createArrayBuilder(
                e.getAllTest().stream().map(Object::toString).collect(Collectors.toList()))))
        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::addAll);

    var result = Json.createObjectBuilder().add("testView", testView)
        .add("entityView", entityView)
        .build();

    return result;
  }
}
