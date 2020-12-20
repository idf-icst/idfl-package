package defects4j;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

/**
 * Metrics that are used for evaluation
 */
public class Metrics {
  double MAP;
  int top1;
  int top5;
  int top10;
  double MRR;
  int algos;

  @Override
  public String toString() {
    var map = MAP / algos;
    var mrr = MRR / algos;
    return String.format("%9.0f", Double.isNaN(map) ? 0.0 : map) + " : "
        + String.format("%9.0f", (double) top1 / algos) + " : "
        + String.format("%9.0f", (double) top5 / algos) + " : "
        + String.format("%9.0f", (double) top10 / algos) + " : "
        + String.format("%9.0f", Double.isNaN(mrr) ? 0.0 : mrr);
  }

  public static Metrics from(List<String> list) {
    return new Metrics(
        Double.parseDouble(list.get(0).trim()),
        Integer.parseInt(list.get(1).trim()),
        Integer.parseInt(list.get(2).trim()),
        Integer.parseInt(list.get(3).trim()),
        Double.parseDouble(list.get(0).trim()),
        1);
  }

  public static Metrics average(List<Metrics> list) {
    var map = Math.round(list.stream().collect(Collectors.averagingDouble(m -> m.MAP)));
    var top1 = (int) Math.round(list.stream().collect(Collectors.averagingInt(m -> m.top1)));
    var top5 = (int) Math.round(list.stream().collect(Collectors.averagingInt(m -> m.top5)));
    var top10 = (int) Math.round(list.stream().collect(Collectors.averagingInt(tom -> tom.top10)));
    return new Metrics(map, top1, top5, top10, map, list.size());
  }

  public String format() {
    return String.format("%d & %d & %.0f", top1, top5, MAP);
  }

}
