package defects4j;

import static defects4j.Mode.*;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class converts raw data from defects4j's fault-localization.cs.washington.edu
 * to spectrum format that can be used by ranking algorithms
 */
public class SpParser {

  static Logger slf4jLogger = LoggerFactory.getLogger(SpParser.class);
  public static final String root = "src/main/resources/defects4j/fault-localization.cs.washington.edu/data";

  public static Map<Mode, Spectrum> parse(String programId, String bugId) throws IOException {
    var dataPath = Paths.get(root, programId, bugId, "gzoltars", programId, bugId);
    var spectra = Files.readAllLines(dataPath.resolve("spectra"), StandardCharsets.ISO_8859_1);
    var fullCoverage = Files.readAllLines(dataPath.resolve("matrix"), StandardCharsets.ISO_8859_1);

    var ftPositions = IntStream.range(0, fullCoverage.size())
        .filter(i -> fullCoverage.get(i).endsWith("-"))
        .boxed()
        .collect(Collectors.toList());

    assert ftPositions.size() > 0;

    var extraPassedTestModeMap = getExtraPassedTests(fullCoverage, ftPositions.get(0));
    var extraFailedTestModeMap = getExtraFailedTests(fullCoverage, ftPositions);

    var temp = Stream.concat(
        extraPassedTestModeMap.entrySet().stream(),
        extraFailedTestModeMap.entrySet().stream())
        .collect(Collectors.toMap(
        Entry::getKey,
        e -> transform(e.getValue(), spectra)));

    temp.put(COMPLETE, transform(fullCoverage, spectra));

    return temp;
  }

  public static Spectrum transform(List<String> coverage, List<String> spectra) {
    var totalEntities = coverage.get(0).split(" ").length - 1;
    var totalTests = coverage.size();

    slf4jLogger.debug("matrix = " + totalEntities + ", " + totalTests);

    var testMap = IntStream.range(0, coverage.size())
        .boxed()
        .collect(toMap(i -> i, i -> coverage.get(i).endsWith("+")));

    var passedTests = testMap.values().stream().filter(b -> b).count();
    var failedTests = testMap.size() - passedTests;

    assert failedTests > 0;

    var coverageMap = IntStream.range(0, coverage.size()).parallel()
        .boxed()
        .collect(
            toMap(i -> i,
                i -> coverage.get(i).substring(0, coverage.get(i).length() - 1)
                    .split(" ")));

    var spectrum = new int[totalEntities][2];
    for (int i = 0; i < totalEntities; i++) {
      var passedCount = 0;
      var failedCount = 0;
      for (int j = 0; j < totalTests; j++) {
        var isCovered = coverageMap.get(j)[i];
        if (isCovered.equals("1")) {
          slf4jLogger.debug("at row: " + j + ", column: " + i + " => " + isCovered);
          var status = testMap.get(j);
          if (status) {
            passedCount++;
          } else {
            failedCount++;
          }
        }
      }
      spectrum[i][0] = passedCount;
      spectrum[i][1] = failedCount;
    }
    return new Spectrum(spectrum, failedTests, passedTests, spectra);
  }

  private static Map<Mode, List<String>> getExtraPassedTests(List<String> fullCoverage,
      int fftPosition) {
    var modes = List
        .of(EXTRA_PASSED_TESTS_1, EXTRA_PASSED_TESTS_2, EXTRA_PASSED_TESTS_3, EXTRA_PASSED_TESTS_4,
            EXTRA_PASSED_TESTS_5, EXTRA_PASSED_TESTS_6, EXTRA_PASSED_TESTS_7, EXTRA_PASSED_TESTS_8,
            EXTRA_PASSED_TESTS_9, EXTRA_PASSED_TESTS_10);

    var firstHalf = fullCoverage.subList(0, fftPosition + 1);

    var secondHalf = List.<String>of();

    if (fftPosition <= fullCoverage.size() - 1) {
      secondHalf = fullCoverage.subList(fftPosition + 1, fullCoverage.size()).stream()
          .filter(c -> c.endsWith("+"))
          .limit(modes.size())
          .collect(Collectors.toList());
    }

    var limit = Math.min(secondHalf.size(), modes.size());

    List<String> finalSecondHalf = List.copyOf(secondHalf);

    return modes.stream()
        .filter(mode -> mode.value <= limit)
        .collect(Collectors.toMap(mode -> mode,
            mode -> Stream.concat(firstHalf.stream(), finalSecondHalf.subList(0, mode.value).stream())
                .collect(Collectors.toList())));
  }

  private static Map<Mode, List<String>> getExtraFailedTests(List<String> fullCoverage,
      List<Integer> ftPositions) {
    var modes = List
        .of(FIRST_FAILED_TEST, SECOND_FAILED_TEST, THIRD_FAILED_TEST, FOURTH_FAILED_TEST,
            FIFTH_FAILED_TEST);
    return modes.stream().filter(mode -> mode.value < ftPositions.size())
        .collect(Collectors
            .toMap(mode -> mode, mode -> fullCoverage.subList(0, ftPositions.get(mode.value) + 1)));
  }
}
