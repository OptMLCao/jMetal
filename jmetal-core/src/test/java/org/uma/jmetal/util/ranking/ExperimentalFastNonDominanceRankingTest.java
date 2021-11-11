package org.uma.jmetal.util.ranking;

import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ranking.impl.ExperimentalFastNonDominanceRanking;

public class ExperimentalFastNonDominanceRankingTest extends NonDominanceRankingTestCases<Ranking<DoubleSolution>> {

    public ExperimentalFastNonDominanceRankingTest() {
        setRanking(new ExperimentalFastNonDominanceRanking<DoubleSolution>());
    }

}

