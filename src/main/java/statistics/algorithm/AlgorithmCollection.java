package statistics.algorithm;

import java.util.function.BiFunction;

public class AlgorithmCollection {

  public static BiFunction<Spectrum, RankingAlgorithm, Double> AlgorithmSelectorFunction = (transformer, rankingAlgorithm) -> {
    switch (rankingAlgorithm) {
      case TARANTULA:
        return transformer.toTarantula();
      case M1:
        return transformer.toM1();
      case M2:
        return transformer.toM2();
      case DICE:
        return transformer.toDice();
      case AMPLE:
        return transformer.toAmple();
      case SOKAL:
        return transformer.toSokal();
      case WONG1:
        return transformer.toWong1();
      case WONG2:
        return transformer.toWong2();
      case WONG3:
        return transformer.toWong3();
      case EUCLID:
        return transformer.toEuclid();
      case HAMANN:
        return transformer.toHamann();
      case OCHIAI:
        return transformer.toOchiai();
      case ZOLTAR:
        return transformer.toZoltar();
      case GOODMAN:
        return transformer.toGoodman();
      case HAMMING:
        return transformer.toHamming();
      case JACCARD:
        return transformer.toJaccard();
      case OCHIAI2:
        return transformer.toOchiai2();
      case OVERLAP:
        return transformer.toOverlap();
      case ANDERBERG:
        return transformer.toAnderberg();
      case RUSSELLRAO:
        return transformer.toRussellRao();
      case KULCZYNSKI1:
        return transformer.toKulczynski1();
      case KULCZYNSKI2:
        return transformer.toKulczynski2();
      case SORENSENDICE:
        return transformer.toSorensenDice();
      case ROGERSTANIMOTO:
        return transformer.toRogersTanimoto();
      case SIMPLEMATCHING:
        return transformer.toSimpleMatching();
      default:
        return null;
    }
  };
}
