package statistics.algorithm;

import statistics.entity.Program;

public interface SpectrumBasedIndexing {

  /**
   * Index all elements by a specific algorithm
   *
   * @param algorithm which algorithm used to rank
   */
  Program indexByAlgorithm(RankingAlgorithm algorithm);
}
