package statistics.experiment;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class Misc {

  private static void parseTime() {

    Arrays.asList("naturalOrder", "quadrantOrder")
        .stream()
        .forEach(type -> {
              var dir = String.format("data/1k/%s/timeTakens", type);

              var content = Arrays.stream(Paths.get(dir).toFile().listFiles())
                  .sorted(
                      Comparator.comparing(file -> Integer.parseInt(file.getName().split("\\.")[0])))
                  .map(file -> FileUtils.readTextFile(file.getAbsolutePath())
                      .map(line -> String.format("%.3f", Double.parseDouble(line.split(" ")[1])))
                      .collect(Collectors.joining("\n")))
                  .collect(Collectors.joining("\n\n\n\n"));

              FileUtils.writeTextToFile(content,
                  String.format("data/1k/%s/timeTakens/time.txt", type));

            }
        );
  }

  public static void main(String[] args) {
    parseTime();
  }

}
