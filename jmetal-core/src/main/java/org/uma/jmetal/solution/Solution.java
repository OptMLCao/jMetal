package org.uma.jmetal.solution;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Interface representing a Solution
 *
 * @param <T> Type (Double, Integer, etc.)
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public interface Solution<T> extends Serializable {
    List<T> variables();

    double[] objectives();

    double[] constraints();

    Map<Object, Object> attributes();

    Solution<T> copy();
}
