package defects4j;

import static defects4j.Configs.analysisDir;
import static defects4j.Configs.delDir;
import static defects4j.Configs.resources;
import static defects4j.Configs.resultsDir;
import static defects4j.Project.constructMetaData;
import static defects4j.Project.getBugs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import statistics.algorithm.RankingAlgorithm;

/**
 * This is the main class that runs the experiments and extract results into human-readable format
 */
public class Evaluation {

  public static void main(String[] args) throws Exception {
    constructMetaData(ExperimentType.SINGLE);
    runExperiment(ExperimentType.SINGLE);
    extractResultsIntoExcel(ExperimentType.SINGLE);
  }

  private static void runExperiment(ExperimentType experimentType) throws Exception {
    cleanResults(experimentType);                     // clean existing result folder
    runMain(experimentType);                          // run experiment and save raw data into files
    evaluatePerformanceInAverage(experimentType);     // read raw results and calculate evaluation metrics for average case over all 25 algorithms
    evaluatePerformanceForIndividual(experimentType); // read raw results and calculate evaluation metrics for individual algorithms
  }

  private static void extractResultsIntoExcel(ExperimentType experimentType) throws IOException {
    var out = resources + "csv";
    contractExcelTableForPassedTestSeries(experimentType, RankingAlgorithm.AMPLE);
    constructExcelTableAverageOnAllAlgorithmsBy(Mode.COMPLETE, ExperimentType.SINGLE, out);
    constructExcelTableAverageOnAllAlgorithmsBy(Mode.FIRST_FAILED_TEST, ExperimentType.SINGLE, out);
  }

  private static void evaluatePerformanceForIndividual(ExperimentType experimentType) {
    Arrays.stream(RankingAlgorithm.values()).forEach(algorithm -> {
      try {
        runAllProjectsGiven(algorithm, experimentType);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /*
    run experiment for all project given algorithm and experiment type
   */
  private static void runAllProjectsGiven(RankingAlgorithm algorithm, ExperimentType experimentType) throws Exception {
    var dir = resources + experimentType.relativePath + analysisDir;
    if (!Files.exists(Paths.get(dir))) {
      Files.createDirectory(Paths.get(dir));
    }
    delDir(dir + "/" + algorithm.toString());
    Files.createDirectory(Paths.get(dir, algorithm.toString()));
    Project.all.keySet().stream().forEach(p -> {
      try {
        analyzeForIndividual(p, algorithm, experimentType);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private static void evaluatePerformanceInAverage(ExperimentType experimentType) throws Exception {
    var dir = resources + experimentType.relativePath + analysisDir + "/average";
    delDir(dir);
    Files.createDirectory(Paths.get(dir));

    Project.all.keySet().stream().forEach(p -> {
      try {
        analyzeForAverage(p, experimentType);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  /*
  for average case
 */
  private static void analyzeForAverage(String pid, ExperimentType experimentType) throws IOException {
    Map<String, Metrics> temp = analyzeAverageAlgorithms(pid, experimentType);

    //bid => bid::mode
    var map = temp.keySet().stream()
        .collect(
            Collectors.groupingBy(e -> Integer.parseInt(e.split("::")[0]), Collectors.toList()));


    var header = String
        .format("%-25s| %10s| %10s| %10s| %10s| %10s| \n", "BugId::Mode", "MAP(%)", "TOP-1(%)", "TOP-5(%)", "TOP-10(%)", "MRR(%)");
    var sptor = IntStream.range(0, header.length()).boxed().map(i -> "-").collect(Collectors.joining(""));

    var data = map.entrySet()
        .stream()
        .sorted(Comparator.comparingInt(Entry::getKey))
        .map(e -> e.getValue().stream()
            .sorted(Comparator.comparingInt(s -> Mode.valueOf(s.split("::")[1]).ordinal()))
            .map(en -> String.format("%-25s", en) + ": " + temp.get(en))
            .collect(Collectors.joining("\n", header + sptor + "\n", "\n")))
        .collect(Collectors.toList());

    var dir = resources + experimentType.relativePath + analysisDir + "/average";
    Files.write(Paths.get(dir, pid), data);
  }

  private static void analyzeForIndividual(String pid, RankingAlgorithm algorithm, ExperimentType experimentType) throws IOException {
    //bid::mode => metrics
    var bidModeMetrics = analyzeIndividualAlgorithms(pid, algorithm, experimentType);

    //bid => bid::mode
    var bidMode = bidModeMetrics.keySet().stream()
        .collect(
            Collectors.groupingBy(e -> Integer.parseInt(e.split("::")[0]), Collectors.toList()));

    var header = String
        .format("%-25s| %10s| %10s| %10s| %10s| %10s| \n", "BugId::Mode", "MAP(%)", "TOP-1(%)", "TOP-5(%)", "TOP-10(%)", "MRR(%)");
    var sptor = IntStream.range(0, header.length()).boxed().map(i -> "-").collect(Collectors.joining(""));

    var data = bidMode.entrySet()
        .stream()
        .sorted(Comparator.comparingInt(Entry::getKey))
        .map(e -> e.getValue().stream()
            .sorted(Comparator.comparingInt(s -> Mode.valueOf(s.split("::")[1]).ordinal()))
            .map(en -> String.format("%-25s", en) + ": " + bidModeMetrics.get(en))
            .collect(Collectors.joining("\n", header + sptor + "\n", "\n")))
        .collect(Collectors.toList());

    var dir = resources + "/" + experimentType.relativePath + analysisDir;
    Files.write(Paths.get(dir, algorithm.toString(), pid), data);
  }

  /*
    run experiment for all projects and save results in files
   */
  private static void runMain(ExperimentType experimentType) throws IOException {
    var out = resources + experimentType.relativePath + resultsDir;

    delDir(out);
    Files.createDirectory(Paths.get(out));

    Project.all.entrySet().parallelStream()
        .map(p -> {
          try {
            return evaluate(getBugs(p.getKey(), experimentType));
          } catch (IOException e) {
            e.printStackTrace();
          }
          return null;
        })
        .filter(Objects::nonNull)
        .flatMap(m -> m.entrySet().parallelStream())
        .filter(en -> !en.getValue().isEmpty())
        .forEach(e -> {
          try {
            saveResults(e.getKey(), e.getValue(), out);
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        });
  }

  public static Map<RankingAlgorithm, Map<Mode, Collection<FaultyLocation>>> evaluate(String pid,
      String bid, Collection<String> groundTruth)
      throws Exception {
    var spectrumMap = SpParser.parse(pid, bid);
    return Arrays.stream(RankingAlgorithm.values())
        .parallel()
        .collect(Collectors
            .toMap(algo -> algo, algo -> evalSingleAlgorithm(algo, spectrumMap, groundTruth)));
  }

  public static Map<RankingAlgorithm, Map<Mode, Collection<FaultyLocation>>> evaluate(Bug bug)
      throws Exception {
    return evaluate(bug.pid, bug.bid, bug.groundTruth);
  }

  public static Map<Bug, Map<RankingAlgorithm, Map<Mode, Collection<FaultyLocation>>>> evaluate(
      Collection<Bug> bugs) {
    return bugs.stream()
        .collect(Collectors.toMap(b -> b, b -> {
          try {
            return evaluate(b);
          } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
          }
        }));
  }

  public static Map<Mode, Collection<FaultyLocation>> evalSingleAlgorithm(
      RankingAlgorithm algorithm, Map<Mode, Spectrum> sp, Collection<String> groundTruth) {
    return sp.entrySet()
        .parallelStream()
        .collect(Collectors.toMap(Entry::getKey,
            e -> Ranker.rank(e.getValue(), algorithm, groundTruth)));
  }

  public static void printResults(
      Map<RankingAlgorithm, Map<Mode, Collection<FaultyLocation>>> results) {
    results.forEach((key, value) -> {
      System.out.println(key);
      value.entrySet().stream()
          .sorted(Comparator.comparingInt(e -> e.getKey().ordinal()))
          .flatMap(e -> e.getValue().stream().map(v -> e.getKey() + " : " + v.toString()))
          .forEach(l -> System.out
              .println("\t\t\t" + l));
    });
  }

  public static void saveResults(Bug bug,
      Map<RankingAlgorithm, Map<Mode, Collection<FaultyLocation>>> results, String outDir)
      throws IOException {
    var formattedResults = results.entrySet()
        .stream()
        .sorted(Comparator.comparingInt(algoEntry -> algoEntry.getKey().ordinal()))
        .flatMap(algEntry -> algEntry.getValue().entrySet()
            .stream()
            .sorted(Comparator.comparingInt(e -> e.getKey().ordinal()))
            .flatMap(e -> e.getValue()
                .stream()
                .map(v -> e.getKey() + " : " + v.toString()))
            .map(l -> algEntry.getKey() + " : " + l)
        )
        .collect(Collectors.toList());
    if (!Files.exists(Paths.get(outDir, bug.pid))) {
      Files.createDirectory(Paths.get(outDir, bug.pid));
    }

    Files.write(Paths.get(outDir, bug.pid, bug.bid), formattedResults);
  }

  public static void cleanResults(ExperimentType experimentType) throws IOException {
    delDir(resources + experimentType.relativePath + resultsDir);
  }

  public static Collection<Result> loadResults(String pid, ExperimentType experimentType) {
    var dir = resources + experimentType.relativePath + resultsDir;

    return Project.all.entrySet().stream().filter(p -> p.getKey().equals(pid)).flatMap(p -> {
      var src = Paths.get(dir, p.getKey());
      try {
        return Files.list(src).flatMap(path -> {
          try {
            return Files.lines(path)
                .map(line -> p.getKey() + " : " + path.getFileName().toString() + " : " + line);
          } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
        return Stream.empty();
      }
    }).map(Result::from).collect(Collectors.toList());
  }

  static BinaryOperator<Metrics> reducer = (m1, m2) -> {
    var map = (m1.MAP + m2.MAP);
    var top1 = (m1.top1 + m2.top1);
    var top5 = (m1.top5 + m2.top5);
    var top10 = (m1.top10 + m2.top10);
    var mrr = (m1.MRR + m2.MRR);
    var algos = m1.algos + m2.algos;
    return new Metrics(map, top1, top5, top10, mrr, algos);
  };

  public static Map<String, Metrics> analyzeAverageAlgorithms(String pid, ExperimentType experimentType) {
    var data = loadResults(pid, experimentType);
    return data.stream()
        .collect(Collectors.groupingBy(r -> r.bid + "::" + r.mode.toString(),
            Collectors.mapping(r -> new Metrics(r.MAP, r.top1, r.top5, r.top10, r.MRR, 1),
                Collectors.reducing(new Metrics(), reducer))));
  }

  public static Map<String, Metrics> analyzeIndividualAlgorithms(String pid, RankingAlgorithm algorithm, ExperimentType experimentType) {
    return loadResults(pid, experimentType).stream().filter(r -> r.algo == algorithm)
        .collect(Collectors.groupingBy(r -> r.bid + "::" + r.mode.toString(),
            Collectors.mapping(r -> new Metrics(r.MAP, r.top1, r.top5, r.top10, r.MRR, 1),
                Collectors.reducing(new Metrics(), reducer))));
  }

  public static void contractExcelTableForPassedTestSeries(ExperimentType experimentType, RankingAlgorithm algorithm) throws IOException {
    var dir = resources + experimentType.relativePath + analysisDir + "/" + algorithm.toString();

    var table = Project.all.entrySet().stream()
        .flatMap(e -> {
          var pid = e.getKey();
          try {
            return Files.lines(Paths.get(dir, pid))
                .filter(line -> !line.isBlank() || !line.isEmpty())
                .filter(line -> !line.equals("\n"))
                .map(line -> pid + "::" + line);
          } catch (IOException ex) {
            ex.printStackTrace();
          }
          return Stream.empty();
        }).peek(System.out::println)
        .collect(Collectors.groupingBy(line -> line.split(" ")[0].split("::")[2].trim().contains(":")
                ? line.split(" ")[0].split("::")[2].trim().split(":")[0].trim()
                : line.split(" ")[0].split("::")[2].trim(),
            Collectors.mapping(line -> line.substring(0, line.lastIndexOf("::")) + line.substring(line.indexOf(" ")),
                Collectors.toMap(line -> line.split(" ")[0].trim(), line -> line.substring(line.indexOf(" ")).trim()))));

    var bugList = table.entrySet()
        .stream()
        .filter(e -> e.getKey().equals(Mode.EXTRA_PASSED_TESTS_10.toString()))
        .flatMap(e -> e.getValue().keySet().stream())
        .sorted(Comparator.comparing(key -> key.split("::")[0]))
        .collect(Collectors.toList());

    var comp = Comparator.comparing(s -> ((String)s).split("::")[0])
        .thenComparingInt(s -> Integer.parseInt(((String)s).split("::")[1]));

    var result = Arrays.stream(Mode.values())
        .filter(mode -> mode.toString().contains("EXTRA_PASSED_TESTS"))
        .sorted(Comparator.comparingInt(Enum::ordinal))
        .map(mode -> {
          var temp = bugList
              .stream()
              .sorted(comp)
              .map(bug -> mode + ", " + bug + ", " + Arrays
              .stream((table.get(mode.toString()).get(bug).startsWith(":")
                  ? table.get(mode.toString()).get(bug).substring(table.get(mode.toString()).get(bug).indexOf(":") + 1)
                  : table.get(mode.toString()).get(bug))
                  .split(":"))
              .map(String::trim)
              .limit(4)
              .collect(Collectors.joining(", ")))
              .collect(Collectors.toList());

          var list = temp.stream()
              .map(line -> Arrays.stream(line.split("::")[1].split(","))
                  .skip(1)
                  .map(String::trim)
                  .map(Integer::parseInt)
                  .collect(Collectors.toList()))
              .collect(Collectors.toList());

          var header = IntStream.range(0, 4)
              .boxed()
              .map(i -> (double) list.stream().map(l -> l.get(i)).reduce(0, Integer::sum) / list.size())
              .map(d -> Math.round(d))
              .map(String::valueOf)
              .collect(Collectors.joining(", ", "average, average, ", ""));

          return Stream.concat(temp.stream(), Stream.of(header))
              .collect(Collectors.joining("\n"));
        })
        .collect(Collectors.joining("\n\n"));

    Files.writeString(Paths.get("src/main/resources/csv", "extra-passed-series.csv"), result);
  }

  private static void constructExcelTableAverageOnAllAlgorithmsBy(Mode tgMode, ExperimentType experimentType, String out) throws IOException {
    var dir = resources + experimentType.relativePath + analysisDir;

    var temp = Arrays.stream(RankingAlgorithm.values())
        .filter(algorithm -> algorithm == RankingAlgorithm.AMPLE)
        .collect(Collectors.toMap(algorithm -> algorithm, algorithm -> {
          var tmp = Project.all.keySet().stream()
              .flatMap(pid -> {
                var path = Paths.get(dir, algorithm.toString(), pid);
                try {
                  return Files.lines(path)
                      .filter(line -> line.contains(tgMode.toString()))
                      .map(line -> Arrays.stream(line.split(tgMode.toString())[1].split(":")).skip(1).limit(4).collect(
                          Collectors.toList()))
                      .map(Metrics::from);
                } catch (IOException e) {
                  e.printStackTrace();
                }
                return Stream.empty();
              })
              .collect(Collectors.toList());
          return Metrics.average(tmp);
        }));

    var content = temp.entrySet().stream().sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
        .map(e -> e.getKey().toString() + ", " + e.getValue().format())
        .collect(Collectors.toList());

    Files.write(Paths.get(out, tgMode.toString() + ".csv"), content);
  }
}
