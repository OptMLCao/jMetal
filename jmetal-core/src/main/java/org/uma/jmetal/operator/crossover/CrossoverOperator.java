package org.uma.jmetal.operator.crossover;

import org.uma.jmetal.operator.Operator;

import java.util.List;

/**
 * Interface representing crossover operators. They will receive a list of solutions and return
 * another list of solutions
 *
 * @param <Source> The class of the solutions
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public interface CrossoverOperator<Source> extends Operator<List<Source>, List<Source>> {
    double getCrossoverProbability();

    int getNumberOfRequiredParents();

    int getNumberOfGeneratedChildren();
}
