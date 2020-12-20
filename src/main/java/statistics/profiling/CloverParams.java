package statistics.profiling;

import java.nio.file.Path;
import java.util.function.Predicate;

public enum CloverParams {
  CLOVER_TEST_TARGETS_STRING("clover.testTargets",
      "this key allows to get a map data from test to coverage data"),
  CLOVER_SRC_FILE_LINES_STRING("clover.srcFileLines",
      "this key allows to get a map data from test to coverage data"),
  MAX_DEPTH(999, "How deep to find the js files which contain actual profiling data"),
  isJsonProfilingDataFile(
      path -> {
        var fileName = path.getFileName().toString();
        return fileName.endsWith(".js")
            && !fileName.contains(".java.")
            && !fileName.endsWith("Test.js");
      }
      , "filter function that returns actual profiling data js files"),
  DEFAULT_PROJECT_PREFIX("com.cvent.eventregistration",
      "Which project or software component to search?"),

  DEFAULT_PROJECT_DIR(
      "flex-event-registration/event-registration-service-web/target/site/clover/com/cvent/eventregistration",
      "Where Clover report files have been generated"),

  TOP_K(100, "Get only top k most suspicious elements");

  String stringValue;
  int intValue;
  String description;
  Predicate<Path> lambda;

  CloverParams(String value, String description) {
    this.stringValue = value;
    this.description = description;
  }

  CloverParams(int intValue, String description) {
    this.intValue = intValue;
    this.description = description;
  }

  CloverParams(Predicate<Path> lambda, String description) {
    this.lambda = lambda;
    this.description = description;
  }

  public static int getMaxDepthSearch() {
    return MAX_DEPTH.intValue;
  }

  public static Predicate<Path> getJsProfilingFileFilter() {
    return isJsonProfilingDataFile.lambda;
  }

  public String getStringValue() {
    return stringValue;
  }

  public int getIntValue() {
    return intValue;
  }
}
