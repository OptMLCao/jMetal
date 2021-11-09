package org.uma.jmetal.util.densityestimator;

import java.util.Comparator;
import java.util.List;

/**
 * Interface representing implementations to compute the crowding distance[拥挤度计算]
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public interface DensityEstimator<S> {

    /* 计算拥挤度 */
    void compute(List<S> solutionSet);

    Double getValue(S solution);

    Comparator<S> getComparator();

}
