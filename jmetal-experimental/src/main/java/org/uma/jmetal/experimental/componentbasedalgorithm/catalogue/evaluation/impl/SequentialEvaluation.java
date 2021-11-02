package org.uma.jmetal.experimental.componentbasedalgorithm.catalogue.evaluation.impl;

import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

/**
 * @param <S>
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class SequentialEvaluation<S extends Solution<?>> extends AbstractEvaluation<S> {
    public SequentialEvaluation(Problem<S> problem) {
        super(new SequentialSolutionListEvaluator<S>(), problem);
    }
}
