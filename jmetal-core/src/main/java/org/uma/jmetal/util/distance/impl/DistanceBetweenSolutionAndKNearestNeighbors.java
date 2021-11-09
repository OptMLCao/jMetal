package org.uma.jmetal.util.distance.impl;

import lombok.extern.slf4j.Slf4j;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.distance.Distance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Class for calculating the Euclidean distance between two {@link DoubleSolution} objects in solution space.
 *
 * @author <antonio@lcc.uma.es>
 */
@Slf4j
public class DistanceBetweenSolutionAndKNearestNeighbors<S extends Solution<?>> implements Distance<S, List<S>> {

    private final int k;
    private Distance<S, S> distance;

    public DistanceBetweenSolutionAndKNearestNeighbors(int k, Distance<S, S> distance) {
        /* 加一个k>0, 若否后续计算会报错 */
        if (k <= 0) {
            log.info("k must >= 0, actually is {}", k);
            throw new IllegalArgumentException();
        }
        this.k = k;
        this.distance = distance;
    }

    public DistanceBetweenSolutionAndKNearestNeighbors(int k) {
        this(k, new EuclideanDistanceBetweenSolutionsInObjectiveSpace<>());
    }

    /**
     * Computes the knn distance. If the solution list size is lower than k, then k = size in the computation
     *
     * @param solution
     * @param solutionList
     * @return
     */
    @Override
    public double compute(S solution, List<S> solutionList) {
        List<Double> listOfDistances = knnDistances(solution, solutionList);
        listOfDistances.sort(Comparator.naturalOrder());
        int limit = Math.min(k, listOfDistances.size());
        double result;
        if (limit == 0) {
            result = 0.0;
        } else {
            result = listOfDistances.get(limit - 1);
        }
        return result;
    }

    /**
     * Computes the distance between a solution and the solutions of a list. Distances equal to 0 are ignored.
     *
     * @param solution
     * @param solutionList
     * @return A list with the distances
     */
    private List<Double> knnDistances(S solution, List<S> solutionList) {
        List<Double> listOfDistances = new ArrayList<>();
        for (int i = 0; i < solutionList.size(); i++) {
            double distanceBetweenSolutions = distance.compute(solution, solutionList.get(i));
            if (distanceBetweenSolutions != 0) {
                listOfDistances.add(distanceBetweenSolutions);
            }
        }
        return listOfDistances;
    }

}
