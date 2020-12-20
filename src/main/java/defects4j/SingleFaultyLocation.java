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
public class SingleFaultyLocation implements FaultyLocation {

  String name;
  int rank;
  double score;

  @Override
  public String toString() {
    return name + " : " + score + " : " + rank + " : "
        + String.format("%.03f", getMAP())
        + " : "
        + List.of(1, 5, 10).stream().map(this::getTop).map(String::valueOf).collect(
        Collectors.joining(" : "));
  }

  public int getTop(int n) {
    return rank <= n ? 1 : 0;
  }

  public double getMAP() {
    assert rank > 0;
    return 1.0 / rank;
  }

  @Override
  public double getMRR() {
    return getMAP();
  }
}
