package org.uma.jmetal.operator.mutation;

import org.uma.jmetal.operator.Operator;

/**
 * Interface representing mutation operators
 *
 * @param <Source> The solution class of the solution to be mutated
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public interface MutationOperator<Source> extends Operator<Source, Source> {
    double getMutationProbability();
}
