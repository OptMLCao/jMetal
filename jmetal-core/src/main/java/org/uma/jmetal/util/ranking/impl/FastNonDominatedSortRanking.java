package org.uma.jmetal.util.ranking.impl;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.comparator.ConstraintViolationComparator;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.ranking.Ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

/**
 * This class implements a solution list ranking based on dominance ranking. Given a collection of
 * solutions, they are ranked according to scheme similar to the one proposed in NSGA-II. As an
 * output, a set of subsets are obtained. The subsets are numbered starting from 0 (in NSGA-II, the
 * numbering starts from 1); thus, subset 0 contains the non-dominated solutions, subset 1 contains
 * the non-dominated population after removing those belonging to subset 0, and so on.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class FastNonDominatedSortRanking<S extends Solution<?>> implements Ranking<S> {

    private String attributeId = getClass().getName();
    private Comparator<S> dominanceComparator;
    /* 约束违反情况比较 */
    private static final Comparator<Solution<?>> CONSTRAINT_VIOLATION_COMPARATOR
            = new ConstraintViolationComparator<Solution<?>>();
    /* 解的排序部分 */
    private List<ArrayList<S>> rankedSubPopulations;

    /**
     * dominanceComparator内部也包含了一个ConstraintViolationComparator.
     * FastNonDominatedSortRanking.CONSTRAINT_VIOLATION_COMPARATOR 和 DominanceComparator.constraintViolationComparator
     * 是一样东西，原作者在这里的设计是有问题还是有意为之？或者是代码修改之后没有统一？ todo 需再确认
     */

    /* Constructor */
    public FastNonDominatedSortRanking(Comparator<S> comparator) {
        this.dominanceComparator = comparator;
        rankedSubPopulations = new ArrayList<>();
    }

    /* Constructor */
    public FastNonDominatedSortRanking() {
        this(new DominanceComparator<>());
    }

    @Override
    public Ranking<S> compute(List<S> solutionList) {

        List<S> population = solutionList;
        // dominateMe[i] contains the number of population dominating i --> 支配解i的解的个数.
        int[] dominateMe = new int[population.size()];
        // iDominate[k] contains the list of population dominated by k  --> 被解k支配的解集.
        List<List<Integer>> iDominate = new ArrayList<>(population.size());
        // front[i] contains the list of individuals belonging to the front i --> pareto非劣解集前沿.
        ArrayList<List<Integer>> front = new ArrayList<>(population.size() + 1);
        /* start 初始化数据准备过程 */
        // Initialize the fronts
        for (int i = 0; i < population.size() + 1; i++) {
            front.add(new LinkedList<Integer>());
        }
        // Fast non dominated sorting algorithm
        // Contribution of Guillaume Jacquenot
        for (int p = 0; p < population.size(); p++) {
            // Initialize the list of individuals that i dominate and the number
            // of individuals that dominate me
            iDominate.add(new LinkedList<Integer>());
            dominateMe[p] = 0;
        }
        /* end 初始化数据准备过程 */
        /* start 确定支配关系 */
        int flagDominate;
        /* 双层for循环看起来复杂度有点高 --> todo 有优化的空间不? */
        for (int p = 0; p < (population.size() - 1); p++) {
            // For all q individuals , calculate if p dominates q or vice versa
            for (int q = p + 1; q < population.size(); q++) {
                /* 感觉这个约束的比较有点多余 */
                flagDominate = CONSTRAINT_VIOLATION_COMPARATOR.compare(solutionList.get(p), solutionList.get(q));
                if (flagDominate == 0) {
                    flagDominate = dominanceComparator.compare(solutionList.get(p), solutionList.get(q));
                }
                if (flagDominate == -1) {
                    iDominate.get(p).add(q);
                    dominateMe[q]++;
                } else if (flagDominate == 1) {
                    iDominate.get(q).add(p);
                    dominateMe[p]++;
                }
            }
        }
        /* 种群中最后一个解的支配关系 */
        for (int i = 0; i < population.size(); i++) {
            if (dominateMe[i] == 0) {
                front.get(0).add(i);
                solutionList.get(i).attributes().put(attributeId, 0);
            }
        }
        /* end 确定支配关系 */
        /* start 进行pareto支配关系排序 */
        // Obtain the rest of fronts
        int i = 0;
        Iterator<Integer> it1, it2; // Iterators
        while (front.get(i).size() != 0) {
            i++;
            it1 = front.get(i - 1).iterator();
            while (it1.hasNext()) {
                it2 = iDominate.get(it1.next()).iterator();
                while (it2.hasNext()) {
                    int index = it2.next();
                    dominateMe[index]--;
                    if (dominateMe[index] == 0) {
                        front.get(i).add(index);
                        solutionList.get(index).attributes().put(attributeId, i);
                    }
                }
            }
        }
        /* end 进行pareto支配关系排序 */
        /* start 对外封装排序后的结果 */
        rankedSubPopulations = new ArrayList<>();
        // 0,1,2,....,i-1 are fronts, then i fronts
        for (int j = 0; j < i; j++) {
            rankedSubPopulations.add(j, new ArrayList<S>(front.get(j).size()));
            it1 = front.get(j).iterator();
            while (it1.hasNext()) {
                rankedSubPopulations.get(j).add(solutionList.get(it1.next()));
            }
        }
        /* end 对外封装排序后的结果 */
        return this;
    }

    @Override
    public List<S> getSubFront(int rank) {
        Check.that(
                rank < rankedSubPopulations.size(),
                "Invalid rank: " + rank + ". Max rank = " + (rankedSubPopulations.size() - 1));

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
