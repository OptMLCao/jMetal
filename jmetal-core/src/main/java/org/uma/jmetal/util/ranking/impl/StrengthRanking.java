package org.uma.jmetal.util.ranking.impl;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.ranking.Ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * This class implements a solution list ranking based on the strength concept defined in SPEA2. The
 * strength of solution is computed by considering the number of solutions they dominates and the
 * strenght of the solutions dominating it. As an output, a set of subsets are obtained. The subsets
 * are numbered starting from 0; thus, subset 0 contains the non-dominated solutions, subset 1
 * contains the non-dominated population after removing those belonging to subset 0, and so on.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */

/**
 * 其他，关于区分可行解 & 不可行解是存在缺点的，求解过程依赖于群体中存在可行，对于可行解比例为0等式问题，
 * 约束问题要装化为不等式约束问题，这必然会影响算法的性能和精度.
 * <p></p>
 * 基于Pareto强度值的个体排序.
 * Pareto强度值演化算法求解约束优化问题-->> 定义1 && 定义2
 */
public class StrengthRanking<S extends Solution<?>> implements Ranking<S> {

    private final String attributeId = getClass().getName();

    /* default populate {@link org.uma.jmetal.util.comparator.DominanceComparator} */
    private Comparator<S> dominanceComparator;

    private List<ArrayList<S>> rankedSubPopulations;

    /* Constructor */
    public StrengthRanking(Comparator<S> comparator) {
        this.dominanceComparator = comparator;
        rankedSubPopulations = new ArrayList<>();
    }

    /* Constructor */
    public StrengthRanking() {
        this(new DominanceComparator<>());
    }

    @Override
    public Ranking<S> compute(List<S> solutionList) {

        int[] strength = new int[solutionList.size()];
        int[] rawFitness = new int[solutionList.size()];
        /* 写反了把？j优于i，j支配i */
        // strength(i) = |{j | j <- SolutionSet and i dominate j}|
        for (int i = 0; i < solutionList.size(); ++i) {
            for (int j = 0; j < solutionList.size(); ++j) {
                if (dominanceComparator.compare(solutionList.get(i), solutionList.get(j)) < 0) {
                    strength[i] += 1.0;
                }
            }
        }

        // Calculate the raw fitness:
        // rawFitness(i) = |{sum strength(j) | j <- SolutionSet and j dominate i}|
        for (int i = 0; i < solutionList.size(); ++i) {
            for (int j = 0; j < solutionList.size(); ++j) {
                /* 这里同理 */
                if (dominanceComparator.compare(solutionList.get(i), solutionList.get(j)) == 1) {
                    rawFitness[i] += strength[j];
                }
            }
        }

        int maxFitnessValue = 0;
        for (int i = 0; i < solutionList.size(); i++) {
            solutionList.get(i).attributes().put(attributeId, rawFitness[i]);
            if (rawFitness[i] > maxFitnessValue) {
                maxFitnessValue = rawFitness[i];
            }
        }

        // front[i] contains the list of individuals belonging to the front i
        rankedSubPopulations = new ArrayList<>(maxFitnessValue + 1);
        IntStream.range(0, maxFitnessValue + 1).forEach(index -> rankedSubPopulations.add(new ArrayList<>()));

        // Assign each solution to its corresponding front
        /* 注意这里是支配强度 */
        solutionList.forEach(solution -> rankedSubPopulations.get((int) solution.attributes().get(attributeId)).add(solution));

        // Remove empty fronts
        // rankedSubPopulations.stream().filter(list -> (list.size() == 0));
        int counter = 0;
        while (counter < rankedSubPopulations.size()) {
            if (rankedSubPopulations.get(counter).size() == 0) {
                rankedSubPopulations.remove(counter);
            } else {
                counter++;
            }
        }
        /**
         * 强度指标反应了个体xi在种群Pt中Pareto优于关系的强弱程度，若xi的强度值越大，则种群中劣于的个体多，
         * 若xi的强度越小，则种群中劣于的个体越少，从而可以根据强度值来对种群中的个体进行排序，对于强度值相等
         * 的情况没，则比较他们违反约束的程度.
         */
        return this;

    }

    @Override
    public List<S> getSubFront(int rank) {
        if (rank >= rankedSubPopulations.size()) {
            throw new JMetalException("Invalid rank: " + rank + ". Max rank = " + (rankedSubPopulations.size() - 1));
        }
        return rankedSubPopulations.get(rank);
    }

    @Override
    public int getNumberOfSubFronts() {
        return rankedSubPopulations.size();
    }

    @Override
    public Integer getRank(S solution) {
        Check.notNull(solution);
        Integer result = -1;
        if (solution.attributes().get(attributeId) != null) {
            result = (Integer) solution.attributes().get(attributeId);
        }
        return result;
    }

    @Override
    public Object getAttributedId() {
        return attributeId;
    }

}
