package defects4j;

import static defects4j.Configs.K;
import static java.util.stream.Collectors.toMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import statistics.algorithm.RankingAlgorithm;

/**
 * This class takes an input spectrum data and ranks each location based on a given algorithm
 */
public class Ranker {

  public static Map<String, Double> rank(Spectrum sp, RankingAlgorithm algorithm) {
    return IntStream.range(0, sp.data.length)
        .boxed()
        .filter(i -> (sp.data[i][0] > 0 || sp.data[i][1] > 0))
        .collect(toMap(i -> sp.spectra.get(i), i -> score(i, sp, algorithm)));
  }

  public static Collection<FaultyLocation> rank(Spectrum sp, RankingAlgorithm algorithm,
      Collection<String> actualBugs) {
    var scores = Ranker.rank(sp, algorithm);
    var rankedList = scores.entrySet().stream()
        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
        .limit(K)
        .collect(Collectors.toList());

    var notFoundSet = actualBugs.stream()
        .filter(g -> rankedList.stream().noneMatch(e -> e.getKey().trim().equals(g.trim())))
        .map(g -> new SingleFaultyLocation(g, K, 0.0));

    var foundSet = IntStream.range(0, rankedList.size())
        .boxed()
        .filter(i -> isActualBug(rankedList.get(i), actualBugs))
        .map(i -> new SingleFaultyLocation(rankedList.get(i).getKey(), i + 1,
            rankedList.get(i).getValue()));

    var allSet = Stream.concat(notFoundSet, foundSet);

    return List.of(new MultipleFaultyLocation(allSet.collect(Collectors.toList())));
  }

  private static boolean isActualBug(Map.Entry<String, Double> location,
      Collection<String> groundTruth) {
    return groundTruth
        .stream()
        .anyMatch(g -> location.getKey().trim().equalsIgnoreCase(g.trim()));
  }

  static Double score(int index, Spectrum sp, RankingAlgorithm algorithm)
      throws UnsupportedOperationException {
    var ep = sp.data[index][0];
    var ef = sp.data[index][1];
    var nf = sp.failedTests;
    var np = sp.passedTests;
    var spVector = new statistics.algorithm.Spectrum(ef, ep, nf, np);
    switch (algorithm) {
      case TARANTULA:
        spVector.toTarantula();
      case AMPLE:
        return spVector.toAmple();
      case EUCLID:
        return spVector.toEuclid();
      case M1:
        return spVector.toM1();
      case M2:
        return spVector.toM2();
      case DICE:
        return spVector.toDice();
      case SOKAL:
        return spVector.toSokal();
      case WONG1:
        return spVector.toWong1();
      case ANDERBERG:
        spVector.toAnderberg();
      case GOODMAN:
        return spVector.toGoodman();
      case HAMANN:
        return spVector.toHamann();
      case JACCARD:
        return spVector.toJaccard();
      case KULCZYNSKI1:
        return spVector.toKulczynski1();
      case KULCZYNSKI2:
        return spVector.toKulczynski2();
      case OCHIAI2:
        return spVector.toOchiai2();
      case ZOLTAR:
        return spVector.toZoltar();
      case ROGERSTANIMOTO:
        return spVector.toRogersTanimoto();
      case WONG3:
        return spVector.toWong3();
      case HAMMING:
        return spVector.toHamming();
      case OCHIAI:
        return spVector.toOchiai();
      case OVERLAP:
        return spVector.toOverlap();
      case RUSSELLRAO:
        return spVector.toRussellRao();
      case WONG2:
        return spVector.toWong2();
      case SIMPLEMATCHING:
        return spVector.toSimpleMatching();
      case SORENSENDICE:
        return spVector.toSorensenDice();
      default:
        throw new UnsupportedOperationException("not yet implemented");
    }
  }

}
