package statistics.experiment;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CsvResultReducer {

  private static String parseCsvData(String csvFilePath) {
    var lines = FileUtils.readTextFile(csvFilePath).collect(Collectors.toList());
    var algos = Arrays.stream(lines.get(0).split(",")).skip(1)
        .map(algo -> algo + "," + "," + ",")
        .collect(Collectors.joining(","));
    var bugs = lines.stream().skip(1)
        .map(bug -> Arrays.stream(bug.split(","))
            .map(rank -> {
              if (!(rank.startsWith("Logic") || rank.startsWith("rBug") || rank
                  .startsWith("mBug"))) {
                var AP = rank.equalsIgnoreCase(">200") ? 0 : 1.0 / Double.parseDouble(rank);
                var top1 = (rank.equalsIgnoreCase(">200") || Integer.parseInt(rank) > 1) ? 0 : 1;
                var top5 = (rank.equalsIgnoreCase(">200") || Integer.parseInt(rank) > 5) ? 0 : 1;
                return String.format("%s,%.3f,%d,%d", rank, AP, top1, top5);
              } else {
                return rank;
              }
            })
            .collect(Collectors.joining(","))
        ).collect(Collectors.joining("\n"));

    var apSums = Arrays.stream(bugs.split("\n"))
        .map(line -> {
          var fields = line.split(",");
          return IntStream.range(1, fields.length).filter(i -> (i % 4) == 2)
              .boxed()
              .map(i -> Double.parseDouble(fields[i]))
              .map(Object::toString)
              .collect(Collectors.joining(","));
        })
        .reduce(CsvResultReducer::apZip)
        .orElseThrow()
        .split(",");

    var top1Sums = Arrays.stream(bugs.split("\n"))
        .map(line -> {
          var fields = line.split(",");

          return IntStream.range(1, fields.length).filter(i -> i % 4 == 3)
              .boxed()
              .map(i -> Integer.parseInt(fields[i]))
              .map(Object::toString)
              .collect(Collectors.joining(","));
        })
        .reduce(CsvResultReducer::topZip)
        .orElseThrow()
        .split(",");

    var top5Sums = Arrays.stream(bugs.split("\n"))
        .map(line -> {
          var fields = line.split(",");

          return IntStream.range(1, fields.length).filter(i -> i % 4 == 0)
              .boxed()
              .map(i -> Integer.parseInt(fields[i]))
              .map(Object::toString)
              .collect(Collectors.joining(","));
        })
        .reduce(CsvResultReducer::topZip)
        .orElseThrow()
        .split(",");

    var summaryLine = "Overall" + "," + IntStream.range(0, apSums.length)
        .boxed()
        .map(i -> "" + "," + Double.parseDouble(apSums[i]) / (lines.size() - 1) + "," + top1Sums[i]
            + "," + top5Sums[i])
        .collect(Collectors.joining(","));

    return "Bugs," + algos + "\n" + bugs + "\n" + summaryLine;
  }

  private static String apZip(String s1, String s2) {
    var a1 = s1.split(",");
    var a2 = s2.split(",");
    return IntStream.range(0, Math.min(a1.length, a2.length))
        .boxed()
        .map((i -> Double.parseDouble(a1[i]) + Double.parseDouble(a2[i])))
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }

  private static String topZip(String s1, String s2) {
    var a1 = s1.split(",");
    var a2 = s2.split(",");
    return IntStream.range(0, Math.min(a1.length, a2.length))
        .boxed()
        .map(i -> Integer.parseInt(a1[i]) + Integer.parseInt((a2[i])))
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }

  public static void massAugumentCsv(String rootDir) {
    Arrays.stream(Objects.requireNonNull(Paths.get(rootDir).toFile().listFiles()))
        .filter(file -> !file.getName().contains("_au"))
        .sorted(Comparator.comparingInt(value -> Integer.parseInt(value.getName().split("\\.")[0])))
        .forEach(file -> {
          var augmentedContent = parseCsvData(file.getAbsolutePath());
          FileUtils.writeTextToFile(augmentedContent,
              Paths.get(rootDir, file.getName().split("\\.")[0] + "_au.csv").toFile()
                  .getAbsolutePath());
        });
  }
}
