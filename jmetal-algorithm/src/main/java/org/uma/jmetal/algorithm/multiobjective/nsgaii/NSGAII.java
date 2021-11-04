package org.uma.jmetal.algorithm.multiobjective.nsgaii;

import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.RankingAndCrowdingSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class NSGAII<S extends Solution<?>> extends AbstractGeneticAlgorithm<S, List<S>> {

    protected final int maxEvaluations;

    protected final SolutionListEvaluator<S> evaluator;

    protected int evaluations;
    protected Comparator<S> dominanceComparator;

    protected int matingPoolSize;
    protected int offspringPopulationSize;

    /**
     * Constructor
     */
    public NSGAII(Problem<S> problem, int maxEvaluations, int populationSize,
                  int matingPoolSize, int offspringPopulationSize,
                  CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator,
                  SelectionOperator<List<S>, S> selectionOperator, SolutionListEvaluator<S> evaluator) {
        this(problem, maxEvaluations, populationSize, matingPoolSize, offspringPopulationSize,
                crossoverOperator, mutationOperator, selectionOperator, new DominanceComparator<S>(), evaluator);
    }

    /**
     * Constructor
     */
    public NSGAII(Problem<S> problem, int maxEvaluations, int populationSize,
                  int matingPoolSize, int offspringPopulationSize,
                  CrossoverOperator<S> crossoverOperator, MutationOperator<S> mutationOperator,
                  SelectionOperator<List<S>, S> selectionOperator, Comparator<S> dominanceComparator,
                  SolutionListEvaluator<S> evaluator) {
        super(problem);
        /* 最大进化代数 */
        this.maxEvaluations = maxEvaluations;
        /* 种群大小 */
        setMaxPopulationSize(populationSize);
        /* 交叉操作 */
        this.crossoverOperator = crossoverOperator;
        /* 变异操作 */
        this.mutationOperator = mutationOperator;
        /* 解的选择 */
        this.selectionOperator = selectionOperator;
        /* 解的评价 */
        this.evaluator = evaluator;
        /* 支配关系比较 */
        this.dominanceComparator = dominanceComparator;
        /* 杂交池 */
        this.matingPoolSize = matingPoolSize;
        /* 子代种群规模 */
        this.offspringPopulationSize = offspringPopulationSize;
    }

    @Override
    protected void initProgress() {
        evaluations = getMaxPopulationSize();
    }

    @Override
    protected void updateProgress() {
        evaluations += offspringPopulationSize;
    }

    @Override
    protected boolean isStoppingConditionReached() {
        return evaluations >= maxEvaluations;
    }

    @Override
    protected List<S> evaluatePopulation(List<S> population) {
        /* 解的评价值就存放在Solution对应的目标字段之中 */
        population = evaluator.evaluate(population, getProblem());

        return population;
    }

    /**
     * This method iteratively applies a {@link SelectionOperator} to the population to fill the mating pool population.
     *
     * @param population
     * @return The mating pool population
     */
    @Override
    protected List<S> selection(List<S> population) {
        List<S> matingPopulation = new ArrayList<>(population.size());
        for (int i = 0; i < matingPoolSize; i++) {
            S solution = selectionOperator.execute(population);
            matingPopulation.add(solution);
        }

        List<S> newList = population.stream().map(solution -> (S) solution.copy()).collect(Collectors.toList());

        return matingPopulation;
    }

    /**
     * This methods iteratively applies a {@link CrossoverOperator} a  {@link MutationOperator} to the population to
     * create the offspring population. The population size must be divisible by the number of parents required
     * by the {@link CrossoverOperator}; this way, the needed parents are taken sequentially from the population.
     * <p>
     * The number of solutions returned by the {@link CrossoverOperator} must be equal to the offspringPopulationSize
     * state variable
     *
     * @param matingPool
     * @return The new created offspring population
     */
    @Override
    protected List<S> reproduction(List<S> matingPool) {
        int numberOfParents = crossoverOperator.getNumberOfRequiredParents();

        checkNumberOfParents(matingPool, numberOfParents);

        List<S> offspringPopulation = new ArrayList<>(offspringPopulationSize);
        for (int i = 0; i < matingPool.size(); i += numberOfParents) {
            List<S> parents = new ArrayList<>(numberOfParents);
            for (int j = 0; j < numberOfParents; j++) {
                parents.add(matingPool.get(i + j));
            }

            List<S> offspring = crossoverOperator.execute(parents);

            for (S s : offspring) {
                mutationOperator.execute(s);
                offspringPopulation.add(s);
                if (offspringPopulation.size() >= offspringPopulationSize)
                    break;
            }
        }
        return offspringPopulation;
    }

    @Override
    protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
        List<S> jointPopulation = new ArrayList<>();
        jointPopulation.addAll(population);
        jointPopulation.addAll(offspringPopulation);
        /* 拥挤度排序选择器 */
        RankingAndCrowdingSelection<S> rankingAndCrowdingSelection;
        rankingAndCrowdingSelection = new RankingAndCrowdingSelection<S>(getMaxPopulationSize(), dominanceComparator);

        return rankingAndCrowdingSelection.execute(jointPopulation);
    }

    @Override
    public List<S> getResult() {
        return SolutionListUtils.getNonDominatedSolutions(getPopulation());
    }

    @Override
    public String getName() {
        return "NSGAII";
    }

    @Override
    public String getDescription() {
        return "Nondominated Sorting Genetic Algorithm version II";
    }
}
