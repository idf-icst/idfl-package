package defects4j;

import static defects4j.Configs.K;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MultipleFaultyLocation implements FaultyLocation {

  List<SingleFaultyLocation> individualActualBug;

  public int getTop(int n) {
    var countTopN = individualActualBug.stream().filter(b -> b.rank <= n).count();
    var totalBuggyLocations = individualActualBug.size();
    return (int) (100.0 * countTopN / totalBuggyLocations);
  }

  public double getMAP() {
    var sortedRanks = individualActualBug.stream()
        .map(b -> b.rank)
        .sorted(Comparator.comparingInt(r -> r))
        .collect(Collectors.toList());

    return (IntStream.range(0, sortedRanks.size())
        .boxed()
        .map(i -> ((double) i + 1) / sortedRanks.get(i))
        .reduce(0.0, Double::sum)) / sortedRanks.size() * 100.0;
  }

  public double getMRR() {
    return 1.0 / (individualActualBug
        .stream()
        .min(Comparator.comparingInt(b -> b.rank))
        .map(b -> b.rank)
        .orElse(K + 1)) * 100.0;
  }

  @Override
  public String toString() {
    var names = individualActualBug.stream().map(b -> b.name).collect(Collectors.joining(","));
    var scores = individualActualBug.stream().map(b -> b.score).map(String::valueOf).collect(
        Collectors.joining(","));
    var ranks = individualActualBug.stream().map(b -> b.rank).map(String::valueOf).collect(
        Collectors.joining(","));
    return (names + " : " + scores + " : " + ranks + " : "
        + String.format("%.03f", getMAP())
        + " : "
        + List.of(1, 5, 10).stream().map(this::getTop).map(String::valueOf).collect(
        Collectors.joining(" : ")))
        + " : "
        + String.format("%.03f", getMRR());
  }
}
