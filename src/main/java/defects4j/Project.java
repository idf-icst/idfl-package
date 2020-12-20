package defects4j;

import static defects4j.Configs.GroundTruthDir;
import static defects4j.Configs.GroundTruthDirSuffix;
import static defects4j.Configs.d4jDataset;
import static defects4j.Configs.delDir;
import static defects4j.Configs.resources;
import static defects4j.Configs.statsDir;
import static defects4j.Configs.zipD4jDatasetDir;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

/**
 * This class defines a project under experiment
 * - pid: project name
 * - bugsTotal: the number of bugs in this dataset
 * - deprecatedBugIds: bugs that are not valid for experiment, i.e., deprecated, no ground-truths found, err in processing data
 */
public class Project {

  static Logger slf4jLogger = LoggerFactory.getLogger(Project.class);

  String pid;
  int bugsTotal;
  int[] deprecatedBugIds;

  public List<Integer> getActiveBugIds() {
    return IntStream.range(1, bugsTotal + 1)
        .filter(i -> Arrays.stream(deprecatedBugIds).noneMatch(dep -> dep == i))
        .boxed()
        .collect(toList());
  }

  public List<Integer> getSingleLocationBugIds() {
    return getActiveBugIds().stream().filter(idx -> {
      var fileName = pid + "-" + idx + GroundTruthDirSuffix;
      var path = Paths.get(GroundTruthDir, fileName);
      System.out.println("reading: " + path.toString());
      try {
        return (Files.lines(path, StandardCharsets.ISO_8859_1).count() == 1);
      } catch (IOException e) {
        slf4jLogger.error("Cannot read ground truth at: " + path.toString());
        return false;
      }
    }).collect(Collectors.toList());
  }

  public static Map<String, Project> all = Map.of(
      "Chart", new Project("Chart", 26, new int[]{6, 13, 23}),
      "Lang", new Project("Lang", 65, new int[]{2, 11, 21, 29, 70, 54, 26, 45, 51, 52, 8, 13, 23, 25, 56, 58, 12, 28}),
      "Math", new Project("Math", 106, new int[]{70, 53, 10, 19, 20, 39, 73, 104, 3, 4, 12, 18, 25, 51, 60, 78, 50}),
      "Mockito", new Project("Mockito", 38, new int[]{1, 7, 8, 4, 15, 30, 31})
  );

  public static Collection<Bug> constructBugs(Project project, Predicate<Bug> filter) {
    return IntStream.range(1, project.bugsTotal)
        .filter(i -> Arrays.stream(project.deprecatedBugIds).noneMatch(d -> d == i))
        .boxed()
        .map(idx -> readThenMap.apply(project.pid, idx))
        .filter(Objects::nonNull)
        .filter(filter)
        .collect(toList());
  }

  private static BiFunction<String, Integer, Bug> readThenMap = (pid, bid) -> {
    var fileName = pid + "-" + bid + GroundTruthDirSuffix;
    var path = Paths.get(GroundTruthDir, fileName);
    try {
      var lines = Files.readAllLines(path, StandardCharsets.ISO_8859_1).stream().map(
          line -> Arrays.stream(line.split("#"))
              .limit(2).collect(Collectors.joining("#"))
              .replace("/", ".").replace(".java", ""))
          .collect(toList());
      return new Bug(pid, String.valueOf(bid), lines);
    } catch (IOException e) {
      slf4jLogger.error("Bug id = [" + pid + "." + bid + "] not found!" + " => " + path.toString());
      return null;
    }
  };

  public static Collection<Bug> getBugs(String pid, ExperimentType experimentType, String... bids) throws IOException {
    var inDir = resources + experimentType.relativePath + d4jDataset;

    return Files.readAllLines(Paths.get(inDir, pid))
        .stream()
        .filter(l -> !l.isEmpty())
        .filter(l -> !l.isBlank())
        .map(l -> {
          var fields = l.split(",");
          return new Bug(fields[0].trim(), fields[1].trim(),
              Arrays.stream(fields).skip(2).map(String::trim).collect(toList()));
        })
        .filter(b -> (bids == null || bids.length == 0) || Arrays.stream(bids)
            .anyMatch(idx -> b.bid.equals(idx)))
        .collect(toList());
  }

  private static Collection<Stat> computeStat(Map.Entry<Project, Collection<Bug>> e) {
    return e.getValue().stream()
        .map(bug -> {
          var path = Paths
              .get(zipD4jDatasetDir, e.getKey().pid, bug.bid, "gzoltars", e.getKey().pid,
                  bug.bid, "matrix");
          try {
            var matrix = Files.readAllLines(path);
            var entities = matrix.get(0).split(" ").length - 1;
            var tests = matrix.size();
            var failedTestPositions = IntStream.range(0, matrix.size())
                .filter(index -> matrix.get(index).endsWith("-"))
                .map(index -> index + 1)
                .boxed()
                .collect(toList());
            return new Stat(e.getKey().pid, bug.bid, entities, tests,
                failedTestPositions, bug.groundTruth.size());
          } catch (IOException ioe) {
            ioe.printStackTrace();
            slf4jLogger.error("Cannot read: " + e.getKey().pid + "." + bug.bid);
          }
          return null;
        }).filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private static void writeStatsToFiles(Map<String, Collection<Stat>> stats, String dir)
      throws IOException {
    delDir(dir);
    Files.createDirectory(Paths.get(dir));

    stats.entrySet().parallelStream()
        .forEach(e -> {
          var pid = e.getKey();
          var path = Paths.get(dir, pid);
          try {
            Files.write(path, e.getValue().stream()
                .sorted(Comparator.comparingInt(stat -> Integer.parseInt(stat.bid)))
                .map(Stat::toString).collect(Collectors.toList()));
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        });
  }

  private static void writeBugsToFiles(Map<Project, Collection<Bug>> map, String dir)
      throws IOException {
    delDir(dir);
    Files.createDirectory(Paths.get(dir));

    map.entrySet().stream().parallel()
        .forEach(e -> {
          var path = Paths.get(dir, e.getKey().pid);
          try {
            Files.write(path, e.getValue().stream().map(Bug::format).collect(toList()));
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        });
  }

  /*
    update both stats and bug map and save to files
   */
  public static void constructMetaData(ExperimentType experimentType) throws IOException {
    var bugsByProject = all.entrySet().parallelStream()
        .collect(Collectors
            .toMap(Entry::getValue, e -> constructBugs(e.getValue(), experimentType.filter)));

    writeBugsToFiles(bugsByProject,resources + experimentType.relativePath + d4jDataset);

    var stats = bugsByProject.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey().pid, Project::computeStat));
    writeStatsToFiles(stats,resources + experimentType.relativePath + statsDir);
  }
}
