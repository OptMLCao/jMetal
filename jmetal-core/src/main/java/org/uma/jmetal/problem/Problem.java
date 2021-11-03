package org.uma.jmetal.problem;

import java.io.Serializable;

/**
 * Interface representing a multi-objective optimization problem
 *
 * @param <S> Encoding
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public interface Problem<S> extends Serializable {

    int getNumberOfVariables();

    /* problem 多个目标数量 */
    int getNumberOfObjectives();

    /* 获取问题的约束 */
    int getNumberOfConstraints();

    /* 问题名称定义 */
    String getName();

    /**
     * This method receives a solution, evaluates it, and returns the evaluated solution.
     *
     * @param solution
     * @return
     */
    S evaluate(S solution);

    S createSolution();
}
