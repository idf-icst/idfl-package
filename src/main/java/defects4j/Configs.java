package defects4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Configs {

  public static final String resources = "src/main/resources/";
  public static final String GroundTruthDirSuffix = ".buggy.lines";
  public static final String GroundTruthDir = "src/main/resources/defects4j/fault-localization-data/analysis/pipeline-scripts/buggy-lines";
  public static final String d4jDataset = "d4jdata";
  public static final String zipD4jDatasetDir = "src/main/resources/defects4j/fault-localization.cs.washington.edu/data";
  public static final String scriptsDir = "scripts";
  public static final String statsDir = "d4jstats";
  public static final String resultsDir = "d4j-results";
  public static final String analysisDir = "d4j-analysis";

  public static final int K = 10000;

  private static Collection<String> genUnzipScript() {
    return Project.all.values().stream()
        .flatMap(project -> IntStream.range(1, project.bugsTotal + 1)
            .filter(id -> Arrays.stream(project.deprecatedBugIds)
                .noneMatch(predicateId -> predicateId == id))
            .boxed()
            .map(id -> {
              var cd = "cd " + zipD4jDatasetDir + "/" + project.pid + "/" + id + ";";
              var cmd = "tar -xzf gzoltar-files.tar.gz;";
              return (cd + "\n" + cmd);
            }))
        .collect(Collectors.toList());
  }

  private static void writeUnzipScript() throws Exception {
    Files
        .write(Paths.get("src/main/resources/scripts", "unzipD4jDs.sh"), genUnzipScript());
  }

  public static void delDir(String dir) throws IOException {
    if (Files.exists(Paths.get(dir))) {
      Files.walk(Paths.get(dir))
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  public static void main(String[] args) throws Exception {
     writeUnzipScript();
  }
}
