package org.uma.jmetal.util.comparator;

import org.uma.jmetal.solution.Solution;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This class implements a <code>Comparator</code> (a method for comparing
 * <code>Solution</code> objects) based whether all the objective values are
 * equal or not. A dominance test is applied to decide about what solution
 * is the best.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class EqualSolutionsComparator<S extends Solution<?>> implements Comparator<S>, Serializable {

    /**
     * Compares two solutions.
     *
     * @param solution1 First <code>Solution</code>.
     * @param solution2 Second <code>Solution</code>.
     * @return -1, or 0, or 1, or 2 if solution1 is dominates solution2(-1), solution1
     * and solution2 are equals(0), or solution1 is greater than solution2(1),
     * respectively.
     */
    @Override
    public int compare(S solution1, S solution2) {
        if (solution1 == null) {
            return 1;
        } else if (solution2 == null) {
            return -1;
        }

        int dominate1; // dominate1 indicates if some objective of solution1
        // dominates the same objective in solution2. dominate2
        int dominate2; // is the complementary of dominate1.

        dominate1 = 0;
        dominate2 = 0;

        int flag;
        double value1, value2;
        for (int i = 0; i < solution1.objectives().length; i++) {
            value1 = solution1.objectives()[i];
            value2 = solution2.objectives()[i];
            /* 目标值比较大小 */
            if (value1 < value2) {
                flag = -1;
            } else if (value1 > value2) {
                flag = 1;
            } else {
                flag = 0;
            }
            /* 解析两个目标值之间的支配关系 */
            if (flag == -1) {
                dominate1 = 1;
            }

            if (flag == 1) {
                dominate2 = 1;
            }
        }
        /* 两个解之间互不支配 */
        if (dominate1 == 0 && dominate2 == 0) {
            //No one dominates the other
            return 0;
        }
        /* 这个是不是有点问题，若上层调用的时候，是两个解交叉调用两次，然后再获取最终结果的话则无问题，若否是不是存在疑问？ */
        /*                                        |                                                    */
        /*                                        |/                                                   */
        /* 没问题的，注意这里写的是 if{} else if {},哈哈哈，对的对的!                                          */
        if (dominate1 == 1) {
            // solution1 dominates
            return -1;
        } else if (dominate2 == 1) {
            // solution2 dominates
            return 1;
        }
        return 2;
    }
}

