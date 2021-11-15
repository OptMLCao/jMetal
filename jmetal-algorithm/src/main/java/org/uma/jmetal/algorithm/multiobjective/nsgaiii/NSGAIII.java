package org.uma.jmetal.algorithm.multiobjective.nsgaiii;

import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.EnvironmentalSelection;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.ReferencePoint;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by ajnebro on 30/10/14.
 * Modified by Juanjo on 13/11/14
 * <p>
 * This implementation is based on the code of Tsung-Che Chiang
 * http://web.ntnu.edu.tw/~tcchiang/publications/nsga3cpp/nsga3cpp.htm
 */
@SuppressWarnings("serial")
public class NSGAIII<S extends Solution<?>> extends AbstractGeneticAlgorithm<S, List<S>> {

    /* 当前迭代次数 */
    protected int iterations;
    /* 最大迭代次数 */
    protected int maxIterations;
    /* 解评价 */
    protected SolutionListEvaluator<S> evaluator;
    /* 沿着每个目标在其方向上的等分数量  */
    protected int numberOfDivisions;
    /* 超平面上的参考点集合 */
    protected List<ReferencePoint<S>> referencePoints = new Vector<>();

    /* Constructor */
    public NSGAIII(NSGAIIIBuilder<S> builder) {
        // can be created from the NSGAIIIBuilder within the same package
        super(builder.getProblem());
        /* 基础运行参数 */
        /* 最大迭代次数 */
        maxIterations = builder.getMaxIterations();
        /* 解的交叉操作 */
        crossoverOperator = builder.getCrossoverOperator();
        /* 解的变异操作 */
        mutationOperator = builder.getMutationOperator();
        /* 解的选择 */
        selectionOperator = builder.getSelectionOperator();
        /* 解的评价 */
        evaluator = builder.getEvaluator();
        /* NSGAIII 用于在每个方向上的等分数量. */
        numberOfDivisions = builder.getNumberOfDivisions();
        /* 参考点生成工具，这个名字不能这个样子命名？太随意了，算了暂时不改 */
        ReferencePoint<S> referencePoint = new ReferencePoint<>();
        assert this.referencePoints.size() == 0;
        /* 获取参考点集，全部的参考点存储在的 referencePoints 之中 */
        referencePoint.generateReferencePoints(referencePoints, getProblem().getNumberOfObjectives(), numberOfDivisions);
        /* start 根据参考点集合中点数量决定种群规模 */
        int populationSize = referencePoints.size();
        while (populationSize % 4 > 0) {
            populationSize++;
        }
        setMaxPopulationSize(populationSize);
        /* end 根据参考点集合中点数量决定种群规模 */
        JMetalLogger.logger.info("referencePointSize: " + referencePoints.size());
    }

    @Override
    protected void initProgress() {
        iterations = 1;
    }

    @Override
    protected void updateProgress() {
        iterations++;
    }

    @Override
    protected boolean isStoppingConditionReached() {
        return iterations >= maxIterations;
    }

    @Override
    protected List<S> evaluatePopulation(List<S> population) {
        population = evaluator.evaluate(population, getProblem());
        return population;
    }

    @Override
    protected List<S> selection(List<S> population) {
        /* 注意：返回的杂交种群的大小等于预订的种群规模 */
        List<S> matingPopulation = new ArrayList<>(population.size());
        for (int i = 0; i < getMaxPopulationSize(); i++) {
            S solution = selectionOperator.execute(population);
            matingPopulation.add(solution);
        }
        return matingPopulation;
    }

    @Override
    protected List<S> reproduction(List<S> population) {
        List<S> offspringPopulation = new ArrayList<>(getMaxPopulationSize());
        for (int i = 0; i < getMaxPopulationSize(); i += 2) {
            List<S> parents = new ArrayList<>(2);
            parents.add(population.get(i));
            parents.add(population.get(Math.min(i + 1, getMaxPopulationSize() - 1)));
            /* 先交叉 */
            List<S> offspring = crossoverOperator.execute(parents);
            /* 再变异 */
            mutationOperator.execute(offspring.get(0));
            mutationOperator.execute(offspring.get(1));
            /* 产生子代种群 */
            offspringPopulation.add(offspring.get(0));
            offspringPopulation.add(offspring.get(1));
        }
        return offspringPopulation;
    }

    /**
     * 参考点拷贝
     *
     * @return
     */
    public List<ReferencePoint<S>> getReferencePointsCopy() {
        List<ReferencePoint<S>> copy = new ArrayList<>();
        for (ReferencePoint<S> r : this.referencePoints) {
            copy.add(new ReferencePoint<>(r));
        }
        return copy;
    }

    @Override
    protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
        /* nsga-II与nsga-III 主要区别就在这里，产生子种群的过程 */
        List<S> jointPopulation = new ArrayList<>();
        jointPopulation.addAll(population);
        jointPopulation.addAll(offspringPopulation);
        /* 进行支配关系排序 */
        Ranking<S> ranking = computeRanking(jointPopulation);
        List<S> last = new ArrayList<>();
        List<S> pop = new ArrayList<>();
        List<List<S>> fronts = new ArrayList<>();
        /* 从支配排序靠前的分层高优添加到子代中，正常情况下不会遍历全部的支持排序层 */
        int rankingIndex = 0;
        int candidateSolutions = 0;
        while (candidateSolutions < getMaxPopulationSize()) {
            /* 从rank=0，自前沿顶部逐步获取 solution */
            last = ranking.getSubFront(rankingIndex);
            fronts.add(last);
            candidateSolutions += last.size();
            if ((pop.size() + last.size()) <= getMaxPopulationSize()) {
                pop.addAll(last);
            }
            rankingIndex++;
        }
        /* 若此时子代种群中个体的数量恰好等于参数设定的种群规模大小阈值 */
        if (pop.size() == this.getMaxPopulationSize()) {
            return pop;
        }
        /* pop种群中个体的数量大于的参数设置的最大种群规模数量的时候启用如下的选择机制 */
        // A copy of the reference list should be used as parameter of the environmental selection
        /* keep note */
        EnvironmentalSelection<S> selection =
                new EnvironmentalSelection<>(fronts, getMaxPopulationSize() - pop.size(),
                        getReferencePointsCopy(), getProblem().getNumberOfObjectives());
        /* 获取亚子代 */
        var choose = selection.execute(last);
        pop.addAll(choose);

        return pop;
    }

    @Override
    public List<S> getResult() {
        return getNonDominatedSolutions(getPopulation());
    }

    protected Ranking<S> computeRanking(List<S> solutionList) {
        /* 获取支配关系，最终支配排序存储在solution.attributes字段中 */
        Ranking<S> ranking = new FastNonDominatedSortRanking<>();
        ranking.compute(solutionList);
        return ranking;
    }

    protected List<S> getNonDominatedSolutions(List<S> solutionList) {
        return SolutionListUtils.getNonDominatedSolutions(solutionList);
    }

    @Override
    public String getName() {
        return "NSGAIII";
    }

    @Override
    public String getDescription() {
        return "Nondominated Sorting Genetic Algorithm version III";
    }

}
