package org.uma.jmetal.util.ranking;

import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

public class FastNonDominatedSortRankingTest extends NonDominanceRankingTestCases<Ranking<DoubleSolution>> {

    public FastNonDominatedSortRankingTest() {
        setRanking(new FastNonDominatedSortRanking<DoubleSolution>());
    }

}

