package org.uma.jmetal.util.densityestimator.impl;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.errorchecking.Check;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 拥挤度比较.
 * This class implements the crowding distance
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class CrowdingDistanceDensityEstimator<S extends Solution<?>> implements DensityEstimator<S> {

    private final String attributeId = getClass().getName();

    /**
     * Assigns crowding distances to all population in a <code>SolutionSet</code>.
     *
     * @param solutionList The <code>SolutionSet</code>.
     */

    @Override
    public void compute(List<S> solutionList) {

        int size = solutionList.size();
        /* start 异常数据处理 */
        if (size == 0) {
            return;
        }
        if (size == 1) {
            solutionList.get(0).attributes().put(attributeId, Double.POSITIVE_INFINITY);
            return;
        }
        if (size == 2) {
            solutionList.get(0).attributes().put(attributeId, Double.POSITIVE_INFINITY);
            solutionList.get(1).attributes().put(attributeId, Double.POSITIVE_INFINITY);
            return;
        }
        /* end 异常数据处理 */
        // Use a new SolutionSet to avoid altering the original solutionSet
        List<S> front = new ArrayList<>(solutionList);
        for (int i = 0; i < size; i++) {
            /* 这里存的是拥挤度信息 */
            front.get(i).attributes().put(attributeId, 0.0);
        }
        /* start 拥挤度排序 */
        int numberOfObjectives = solutionList.get(0).objectives().length;
        /* 逐个目标累加拥挤度 */
        for (int i = 0; i < numberOfObjectives; ++i) {
            // Sort the population by Obj n
            /* 单目标的排序，ObjectiveComparator 内部装配的是升序 -->> 先排序 */
            front.sort(new ObjectiveComparator<>(i));
            // It may be beneficial to change this according to https://dl.acm.org/citation.cfm?doid=2463372.2463456.
            // The additional change that may be beneficial is that if we have only two distinct objective values,
            // we also don't update the crowding distance, as they all will "go to eleven",
            // which makes no sense as this objective just appears to be non-discriminating.
            double minObjective = front.get(0).objectives()[i];
            double maxObjective = front.get(front.size() - 1).objectives()[i];
            if (minObjective == maxObjective) {
                // otherwise all crowding distances will be NaN = 0.0 / 0.0 except for two.
                /*  若最大最小两个极值的value相同，那么该目标无拥挤度距离. */
                continue;
            }
            // Set the crowding distance for the extreme points
            /* 最小的目标值，设置为无穷大 */
            front.get(0).attributes().put(attributeId, Double.POSITIVE_INFINITY);
            /* 最大的目标值，设置为无穷大. */
            front.get(size - 1).attributes().put(attributeId, Double.POSITIVE_INFINITY);

            // Increase the crowding distances for all the intermediate points
            for (int j = 1; j < size - 1; j++) {
                double distance = front.get(j + 1).objectives()[i] - front.get(j - 1).objectives()[i];
                /* 归一化处理 */
                distance = distance / (maxObjective - minObjective);
                distance += (double) front.get(j).attributes().get(attributeId);
                /* 最终遍历完全部的目标后，就完成了全部目标拥挤度获取. */
                front.get(j).attributes().put(attributeId, distance);
            }
        }
        /* end 拥挤度排序 */
        /**
         * 拥挤度：
         * 为了使得到的解在目标解空间中更加均匀，这里引入了拥挤度 nd 概念。
         * 具体算法如下：
         * 1.令参数 nd = 0，n属于 1...N
         * 2.for 每个目标函数 Fm：
         * （1）根据该目标函数对该等级的个体进行排序，记 Fm_max为个体目标函数的最大值，Fm_min为个体目标函数值的最小值；
         * （2）对于排序后两个边界的拥挤度Ld 和 Nd设置无穷大；
         * （3）计算 Nd = Nd + (Fm(i+1)-Fm(i-1))/(Fm_max - Fm_min)，其中Fm(i+1)是排序后后一个个体对应的第m个目标；
         *  同理，Fm(i-1)是排序后前一个个体对应的第m个目标;
         */

    }

    @Override
    public Double getValue(S solution) {
        /* 获取拥挤度 */
        Check.notNull(solution);
        Double result = 0.0;
        if (solution.attributes().get(attributeId) != null) {
            result = (Double) solution.attributes().get(attributeId);
        }
        return result;
    }

    @Override
    public Comparator<S> getComparator() {
        return Comparator.comparing(this::getValue).reversed();
    }

}
