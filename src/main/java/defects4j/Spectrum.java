package defects4j;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

/**
 * - This class captures the spectrum of a d4j program
 * - data [m, 2], where m = number of the program's covered locations;
 * - data[i,0] is the number of passed tests covering location i, data[i,1] is the number of failed tests covering i
 * - spectra contains a real full qualify name of a location, i.e., a location i will have the name spectra.get(i)
 */
public class Spectrum {
  int[][] data;
  long failedTests;
  long passedTests;
  List<String> spectra;
}
