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
 * A POJO class to capture some basic information of a bug, such as,
 * - pid: project name
 * - bid: bug id
 * - entitiesTotal: total locations covered by tests
 * - failedTestPositions: orders of failed tests
 * - faultyLocationsTotal: how many faulty locations of a given bug
 */
public class Stat {

  String pid;
  String bid;
  int entitiesTotal;
  int testsTotal;
  List<Integer> failedTestPositions;
  int faultyLocationsTotal;

  @Override
  public String toString() {
    return (pid + ", " + bid + ", " + entitiesTotal + ", " + testsTotal + ", "
        + failedTestPositions.stream().map(String::valueOf)
        .collect(Collectors.joining(", ", "[", "]"))
        + ", " + faultyLocationsTotal);
  }
}
