package statistics.profiling;

import statistics.entity.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class LegacyDataParser {

  /**
   * Create a map from "TestName" -> "List of covered element's names"
   */
  public static Map<String, List<String>> parseCoverageData(String rawTxtFile) throws IOException {
    List<String> lines = Files.lines(Paths.get(rawTxtFile))
        .filter(p -> p.trim().contains("[java]"))
        .map(p -> p.trim().substring("[java]".length()).trim())
        .collect(Collectors.toList());

    Iterator<String> iterator = lines.iterator();
    Map<String, List<String>> coverageMap = new HashMap<>();

    String lastKey = "";
    while (iterator.hasNext()) {
      String line = iterator.next();
      if (line.startsWith("<Test>")) {
        lastKey = line.split("<Test>")[1].trim();
        coverageMap.put(lastKey, new ArrayList<>());
      } else {
        coverageMap.get(lastKey).add(line);
      }
    }
    return coverageMap;
  }

  /**
   * Load all profiling entity at all levels available in a coverage entity directory
   */
  public static Program loadAllProfilingData(String coverageDir) {
    return loadProfilingDataByCategory(coverageDir, AnalysisLevel.values());
  }

  public static Program loadProfilingDataByCategory(String coverageDir, AnalysisLevel... levels) {
    return Arrays.stream(Objects.requireNonNull(new File(coverageDir)
        .listFiles(f -> f.getName().endsWith(".entity") && f.getName().contains("Coverage"))))
        .collect(Collectors.toMap(f -> AnalysisLevel
            .valueOf(f.getName().split("\\.")[0].split("Coverage")[0].toUpperCase()), f -> {
          try {
            return parseCoverageData(f.getAbsolutePath());
          } catch (IOException e) {
            e.printStackTrace();
          }
          return null;
        })).entrySet()
        .stream()
        .filter(en -> new HashSet<>(Arrays.asList(levels)).contains(en.getKey()))
        .map(entry -> buildExecutionMap(entry.getKey(), entry.getValue()))
        .reduce(new Program(), Program::addProgram);
  }

  public static Program buildExecutionMap(AnalysisLevel level,
      Map<String, List<String>> coverageData) {
    Map<String, AbstractEntity> entities = new HashMap<>();
    Set<TestCase> tests = new HashSet<>();
    coverageData.forEach((key, value) -> {
      TestCase t = new TestCase(key);
      tests.add(t);
      value.stream().filter(e -> !e.contains(".tests."))
          .forEach(e -> {
            String entityName = e.substring(0, e.lastIndexOf('-'));
            int count = Integer.parseInt(e.substring(e.lastIndexOf('-') + 1));
            if (entities.containsKey(entityName)) {
              entities.get(entityName).addTest(t, count);
            } else {
              AbstractEntity entity = ExecutionEntity.createEntity(t, entityName, count, level);
              entities.put(entityName, entity);
            }
            t.addCoveredEntity(entities.get(entityName));
          });

    });
    return new Program(new HashSet<>(entities.values()), tests);
  }

}
