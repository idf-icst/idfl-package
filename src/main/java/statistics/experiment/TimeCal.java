package statistics.experiment;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TimeCal {

  public static void extractTime() {
    var baseDir = "data/1k/naturalOrder/setups";
    var header = "More Passes," + IntStream.range(0, 11).boxed().map(Object::toString)
        .collect(Collectors.joining(","));

    var lines = Arrays.stream(Objects.requireNonNull(Paths.get(baseDir).toFile().listFiles()))
        .sorted(
            Comparator.comparing(id -> Integer.valueOf(id.getName().split("\\.")[0].split("-")[1])))
        .map(bugId -> {
          var testList = FileUtils.readTextFile(bugId.getAbsolutePath()).collect(toList());
          var firstPosition = IntStream.range(0, testList.size()).boxed()
              .filter(i -> testList.get(i).startsWith("*")).findFirst().get();
          return bugId.getName().split("\\.")[0] + "," + IntStream.range(1, 12).boxed()
              .map(i -> testList.subList(0, firstPosition + i)
                  .stream()
                  .map(line -> Double.valueOf(line.split(":")[1]))
                  .reduce(0.0, Double::sum))
              .map(d -> String.format("%.3f", d))
              .collect(joining(","));
        })
        .collect(Collectors.joining("\n"));
    var content = header + "\n" + lines;
    FileUtils
        .writeTextToFile(content, "data/1k/naturalOrder/time/time.csv");
  }

  public static void reduce(String baseDir, String destFile) {
    var content = Arrays.stream(Objects.requireNonNull(Paths.get(baseDir).toFile().listFiles()))
        .sorted(Comparator
            .comparing(fileId -> Integer.valueOf(fileId.getName().split("\\.")[0].split("-")[1])))
        .map(bugId -> {
          var time = FileUtils.readTextFile(bugId.getAbsolutePath())
              .map(line -> Double.valueOf(line.split(":")[1]))
              .reduce(0.0, Double::sum);
          var line = bugId.getName().split("\\.")[0] + "," + String.format("%.3f", time);
          return line;
        })
        .collect(joining("\n"));

    FileUtils.writeTextToFile(content, destFile);

  }
}
