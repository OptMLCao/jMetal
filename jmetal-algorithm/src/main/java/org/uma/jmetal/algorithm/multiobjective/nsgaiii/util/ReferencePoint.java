package org.uma.jmetal.algorithm.multiobjective.nsgaiii.util;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ajnebro on 5/11/14.
 * Modified by Juanjo on 13/11/14
 * This implementation is based on the code of Tsung-Che Chiang
 * http://web.ntnu.edu.tw/~tcchiang/publications/nsga3cpp/nsga3cpp.htm
 */
public class ReferencePoint<S extends Solution<?>> {

    public List<Double> position;
    private int memberSize;
    private List<Pair<S, Double>> potentialMembers;

    public ReferencePoint() {
    }

    /**
     * Constructor
     *
     * @param size multiple Objective size.
     */
    public ReferencePoint(int size) {
        position = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            position.add(0.0);
        }
        memberSize = 0;
        potentialMembers = new ArrayList<>();
    }

    public ReferencePoint(ReferencePoint<S> point) {
        position = new ArrayList<>(point.position.size());
        for (Double d : point.position) {
            position.add(d);
        }
        memberSize = 0;
        potentialMembers = new ArrayList<>();
    }

    /**
     * 生成参考点阵列.
     *
     * @param referencePoints    参考点.
     * @param numberOfObjectives 目标个数.
     * @param numberOfDivisions  目标向量方向上等分的份数.
     */
    public void generateReferencePoints(List<ReferencePoint<S>> referencePoints,
                                        int numberOfObjectives, int numberOfDivisions) {
        ReferencePoint<S> refPoint = new ReferencePoint<>(numberOfObjectives);
        generateRecursive(referencePoints, refPoint, numberOfObjectives, numberOfDivisions, numberOfDivisions, 0);
    }

    /**
     * 递归生成整个参考平面.
     *
     * @param referencePoints    手动维护一个队列用于存储递归获取的参考点集合.
     * @param refPoint           最初参考点.
     * @param numberOfObjectives 优化多目标的个数.--> 进化目标的数量决定了参考点的想向量维数.
     * @param left               开始的游标.
     * @param total              一个目标的方向上等分的份数.
     * @param element            当前填充的参考点对应目标的维度.
     */
    private void generateRecursive(List<ReferencePoint<S>> referencePoints, ReferencePoint<S> refPoint,
                                   int numberOfObjectives, int left, int total, int element) {
        if (element == (numberOfObjectives - 1)) {
            refPoint.position.set(element, (double) left / total);
            referencePoints.add(new ReferencePoint<>(refPoint));
        } else {
            /* i += 1 --> ++i */
            for (int i = 0; i <= left; ++i) {
                refPoint.position.set(element, (double) i / total);
                generateRecursive(referencePoints, refPoint,
                        numberOfObjectives, left - i, total, element + 1);
            }
        }
    }

    public List<Double> pos() {
        return this.position;
    }

    public int MemberSize() {
        return memberSize;
    }

    public boolean HasPotentialMember() {
        return potentialMembers.size() > 0;
    }

    public void clear() {
        memberSize = 0;
        this.potentialMembers.clear();
    }

    public void AddMember() {
        this.memberSize++;
    }

    public void AddPotentialMember(S member_ind, double distance) {
        this.potentialMembers.add(new ImmutablePair<S, Double>(member_ind, distance));
    }

    public void sort() {
        this.potentialMembers.sort(Comparator.comparing(Pair<S, Double>::getRight).reversed());
    }

    /* 注意这里先执行上面的sort，且是反序排列，那么排在最后面的个体，也就是距离当前参考点正交距离最短的个体. */
    public S FindClosestMember() {
        return this.potentialMembers.remove(this.potentialMembers.size() - 1).getLeft();
    }

    public S RandomMember() {
        int index = this.potentialMembers.size() > 1 ?
                JMetalRandom.getInstance().nextInt(0, this.potentialMembers.size() - 1) : 0;
        return this.potentialMembers.remove(index).getLeft();
    }

}
