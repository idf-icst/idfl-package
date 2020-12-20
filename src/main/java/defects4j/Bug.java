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
public class Bug {

  String pid;
  String bid;
  List<String> groundTruth;

  @Override
  public String toString() {
    return "[Project = " + pid + "][BugId = " + bid + "]";
  }

  public boolean isSingleLocation() {
    return groundTruth.size() == 1;
  }

  public boolean isMultiple() {
    return groundTruth.size() > 1;
  }

  public String view() {
    return toString() + "\n" + groundTruth.stream().collect(
        Collectors.joining("\n\t\t\t", "\t\t\t", ""));
  }

  public void print() {
    System.out.println(view());
  }

  public String format() {
    return pid + ", " + bid + ", " + String.join(", ", groundTruth);
  }
}
