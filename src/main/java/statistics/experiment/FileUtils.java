package statistics.experiment;

import javax.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static statistics.experiment.JsonUtils.JSON_TYPE.AS_JSON_OBJECT;
import static statistics.experiment.JsonUtils.parseJsonFromLocalFile;

public class FileUtils {

  public static List<Path> searchFileRecursively(String baseDir, Predicate<Path> filter)
      throws IOException {
    int maxDepth = 999;
    return Files
        .find(Paths.get(baseDir), maxDepth, (p, bfa) -> bfa.isRegularFile() && filter.test(p))
        .collect(toList());
  }

  public static JsonObject fuseContentFromFilesInDir(String baseDir, Predicate<Path> filter)
      throws IOException {
    JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
    searchFileRecursively(baseDir, filter)
        .forEach(path -> jsonObjectBuilder.add(path.getFileName().toString().split("\\.")[0],
            parseJsonFromLocalFile(path.toAbsolutePath().toString(), AS_JSON_OBJECT)
                .map(JsonValue::asJsonObject)
                .orElse(JsonValue.EMPTY_JSON_OBJECT)));
    return jsonObjectBuilder.build();
  }

  public static String stripDoubleQuotes(String s) {
    return s.replaceAll("^\"|\"$", "");
  }

  public static List<String> readLinesFromInputStream(InputStreamReader inputStreamReader,
      Predicate<String> filterFunction, Function<String, String> mapFunction) throws IOException {
    return readLinesFromInputStream(inputStreamReader)
        .stream()
        .filter(filterFunction)
        .map(mapFunction)
        .collect(toList());
  }

  public static List<String> readLinesFromInputStream(InputStreamReader inputStreamReader)
      throws IOException {
    try (BufferedReader br = new BufferedReader(inputStreamReader)) {
      return br.lines().collect(toList());
    }
  }

  public static Stream<String> readTextFile(String filePath) {
    try {
      return Files.lines(Paths.get(filePath), StandardCharsets.ISO_8859_1);
    } catch (IOException e) {
      System.out.println("UTF_8 does not work");
      return Stream.empty();
    }
  }

  public static List<String> readTextFile(String filePath, Predicate<String> filterFunction,
      Function<String, String> mapFunction) {
    return readTextFile(filePath).filter(filterFunction).map(mapFunction).collect(toList());
  }

  public static Stream<File> dfsVisitFile(File file, boolean parallel,
      Comparator<File> comparator) {
    return file.listFiles() == null ? Stream.of(file)
        : (parallel ? Arrays.stream(Objects.requireNonNull(file.listFiles())).sorted(comparator)
            .parallel().flatMap(f -> FileUtils.dfsVisitFile(f, true, comparator))
            : Arrays.stream(Objects.requireNonNull(file.listFiles())).sorted(comparator)
                .flatMap(f -> FileUtils.dfsVisitFile(f, false, comparator)));
  }

  private static <T> void bfsVisitFile(LinkedList<File> linkedList, Function<File, T> mapper,
      Consumer<T> consumer) {
    while (!linkedList.isEmpty()) {
      var file = linkedList.poll();
      consumer.accept(mapper.apply(file));
      if (file.listFiles() != null) {
        Collections.addAll(linkedList, Objects.requireNonNull(file.listFiles()));
      }
    }
  }

  private static <T> void bfsVisitFile(File root, Function<File, T> mapper, Consumer<T> consumer) {
    var linkList = new LinkedList<File>();
    linkList.add(root);
    bfsVisitFile(linkList, mapper, consumer);
  }

  private static <T> void bfsVisitFile(LinkedList<File> linkedList, Function<File, T> mapper,
      List<T> result) {
    while (!linkedList.isEmpty()) {
      var file = linkedList.poll();
      if (file.listFiles() != null) {
        Collections.addAll(linkedList, Objects.requireNonNull(file.listFiles()));
      }
      Collections.addAll(result, mapper.apply(file));
    }
  }

  private static <T> void bfsVisitFile(File root, LinkedList<File> linkedList,
      Function<File, T> mapper, Map<Integer, List<T>> result) {
    while (!linkedList.isEmpty()) {
      var file = linkedList.poll();
      if (file.listFiles() != null) {
        Collections.addAll(linkedList, Objects.requireNonNull(file.listFiles()));
      }
      var key = root.toURI().relativize(file.toURI()).getPath().split("/").length;
      var value = mapper.apply(file);
      if (result.containsKey(key)) {
        result.get(key).add(value);
      } else {
        var list = new ArrayList<T>();
        list.add(value);
        result.put(key, list);
      }
    }
  }

  public static <T> List<T> bfsVisitFileToList(File root, Function<File, T> mapper) {
    var result = new ArrayList<T>();
    var init = new LinkedList<File>();
    init.add(root);
    bfsVisitFile(init, mapper, result);
    return result;
  }

  public static <T> Map<Integer, List<T>> bfsVisitFileToMap(File root, Function<File, T> mapper) {
    var result = new HashMap<Integer, List<T>>();
    var init = new LinkedList<File>();
    init.add(root);
    bfsVisitFile(root, init, mapper, result);
    return result;
  }

  private static String times(String s, int time) {
    return IntStream.range(0, time)
        .mapToObj(i -> s)
        .collect(joining());
  }

  private static void performanceTest(int upperBound, Comparator<File> comparator) {
    var startDir = "./";
    var offsetPath = "../";

    Consumer<String> consumer = dir -> {
      var start = System.currentTimeMillis();
      var files = dfsVisitFile(new File(dir), true, comparator);
      var count = files.count();
      var stop = System.currentTimeMillis();
      var parallel = stop - start;

      var start1 = System.currentTimeMillis();
      var files1 = dfsVisitFile(new File(dir), false, comparator);
      count = files1.count();
      var stop1 = System.currentTimeMillis();
      var notParallel = stop1 - start1;

      System.out.println(String.format(
          "Root: %s, number of files: %d, time taken: %d milSeconds (parallel) - %d milSeconds (NOT parallel)",
          dir, count, parallel, notParallel));
    };

    var start = System.currentTimeMillis();
    IntStream.range(0, upperBound)
        .mapToObj(i -> startDir + times(offsetPath, i))
        .forEach(consumer);
    var stop = System.currentTimeMillis();

    System.out.println("Total (NOT parallel): " + (stop - start));

    start = System.currentTimeMillis();
    IntStream.range(0, upperBound)
        .mapToObj(i -> startDir + times(offsetPath, i))
        .parallel()
        .forEach(consumer);
    stop = System.currentTimeMillis();

    System.out.println("Total (parallel): " + (stop - start));
  }

  public static boolean writeTextToFile(String text, String targetFile) {
    try {
      Files.write(Paths.get(targetFile), text.getBytes());
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

}
