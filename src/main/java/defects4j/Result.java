package defects4j;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import statistics.algorithm.RankingAlgorithm;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

/**
 * A POJO class that captures ranking results of a faulty location found
 */
public class Result {
  String pid;
  String bid;
  RankingAlgorithm algo;
  Mode mode;
  String buggyLocation;
  String score;
  String rank;
  double MAP;
  int top1;
  int top5;
  int top10;
  double MRR;

  public static Result from(String line) {
    var fields = Arrays.stream(line.split(":")).map(String::trim).collect(Collectors.toList());
    return new Result(fields.get(0),
        fields.get(1),
        RankingAlgorithm.valueOf(fields.get(2)),
        Mode.valueOf(fields.get(3)),
        fields.get(4),
        fields.get(5),
        fields.get(6),
        Double.parseDouble(fields.get(7)),
        Integer.parseInt(fields.get(8)),
        Integer.parseInt(fields.get(9)),
        Integer.parseInt(fields.get(10)),
        Double.parseDouble(fields.get(11)));
  }

  @Override
  public String toString() {
    return pid + " : "
        + bid + " : "
        + algo.toString() + " : "
        + mode.toString() + " : "
        + buggyLocation + " : "
        + score + " : "
        + rank + " : "
        + MAP + " : "
        + top1 + " : "
        + top5 + " : "
        + top10 + " : "
        + MRR;
  }
}
