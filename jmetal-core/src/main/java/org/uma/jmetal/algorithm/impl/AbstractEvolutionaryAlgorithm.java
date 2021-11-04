package org.uma.jmetal.algorithm.impl;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;

import java.util.List;

/**
 * Abstract class representing an evolutionary algorithm
 * 进化算法使用的抽象父类.
 *
 * @param <S> Solution
 * @param <R> Result
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public abstract class AbstractEvolutionaryAlgorithm<S, R> implements Algorithm<R> {

    /* 种群 */
    protected List<S> population;
    /* 计算的问题 */
    protected Problem<S> problem;

    public List<S> getPopulation() {
        return population;
    }

    public void setPopulation(List<S> population) {
        this.population = population;
    }

    public void setProblem(Problem<S> problem) {
        this.problem = problem;
    }

    public Problem<S> getProblem() {
        return problem;
    }

    protected abstract void initProgress();

    protected abstract void updateProgress();

    /* 算法终止条件判断 */
    protected abstract boolean isStoppingConditionReached();

    /* 构造初始解 */
    protected abstract List<S> createInitialPopulation();

    /* 评价解 */
    protected abstract List<S> evaluatePopulation(List<S> population);

    /* 解的选择 */
    protected abstract List<S> selection(List<S> population);

    /* 产生子种群 */
    protected abstract List<S> reproduction(List<S> population);

    /* 选择子代进入下一次迭代 */
    protected abstract List<S> replacement(List<S> population, List<S> offspringPopulation);

    @Override
    public abstract R getResult();

    @Override
    public void run() {
        /* run中存在的为算法执行的主流程 */
        /* 子种群 */
        List<S> offspringPopulation;
        /* 杂交种群 */
        List<S> matingPopulation;
        /* 构造初始解 */
        population = createInitialPopulation();
        /* 评价解 */
        population = evaluatePopulation(population);
        /* 算法初始化 */
        initProgress();
        while (!isStoppingConditionReached()) {
            /* 选择并构建种群 */
            matingPopulation = selection(population);
            /* 在当前解领域进行搜索（以遗传为例进行交、变异等操作） */
            offspringPopulation = reproduction(matingPopulation);
            /* 对解进行评价 */
            offspringPopulation = evaluatePopulation(offspringPopulation);
            population = replacement(population, offspringPopulation);
            updateProgress();
        }
    }
}
