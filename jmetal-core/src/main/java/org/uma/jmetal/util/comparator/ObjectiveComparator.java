package org.uma.jmetal.util.comparator;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This class implements a comparator based on a given objective, 类似目标compare的集合.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class ObjectiveComparator<S extends Solution<?>> implements Comparator<S>, Serializable {

    public enum Ordering {

        ASCENDING,  /* 升序 */
        DESCENDING  /* 降序 */

    }

    private int objectiveId;

    private Ordering order;

    /**
     * Constructor.
     *
     * @param objectiveId The index of the objective to compare
     */
    public ObjectiveComparator(int objectiveId) {
        this.objectiveId = objectiveId;
        order = Ordering.ASCENDING;
    }

    /**
     * Comparator.
     *
     * @param objectiveId The index of the objective to compare
     * @param order       Ascending or descending order
     */
    public ObjectiveComparator(int objectiveId, Ordering order) {
        this.objectiveId = objectiveId;
        this.order = order;
    }

    /**
     * Compares two solutions according to a given objective.
     *
     * @param solution1 The first solution
     * @param solution2 The second solution
     * @return -1, or 0, or 1 if solution1 is less than, equal, or greater than solution2,
     * respectively, according to the established order
     */
    @Override
    public int compare(S solution1, S solution2) {
        int result;
        /* 到else{} 之前都是对异常&空值的处理. */
        if (solution1 == null) {
            if (solution2 == null) {
                result = 0;
            } else {
                result = 1;
            }
        } else if (solution2 == null) {
            result = -1;
        } else if (solution1.objectives().length <= objectiveId) {
            throw new JMetalException("The solution1 has " + solution1.objectives().length + " objectives "
                    + "and the objective to sort is " + objectiveId);
        } else if (solution2.objectives().length <= objectiveId) {
            throw new JMetalException("The solution2 has " + solution2.objectives().length + " objectives "
                    + "and the objective to sort is " + objectiveId);
        } else {
            double objective1 = solution1.objectives()[this.objectiveId];
            double objective2 = solution2.objectives()[this.objectiveId];
            /* 看起来写的比较死，可以改成--> 从外部传入一个比较器，由具体注入的compare代理器完成比较工作是不是更好？ */
            if (order == Ordering.ASCENDING) {
                result = Double.compare(objective1, objective2);
            } else {
                result = Double.compare(objective2, objective1);
            }
        }
        return result;
    }

}
