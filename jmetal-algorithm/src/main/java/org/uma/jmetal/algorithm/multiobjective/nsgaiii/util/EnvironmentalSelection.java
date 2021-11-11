package org.uma.jmetal.algorithm.multiobjective.nsgaiii.util;

import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.solutionattribute.SolutionAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class EnvironmentalSelection<S extends Solution<?>> implements SelectionOperator<List<S>, List<S>>,
        SolutionAttribute<S, List<Double>> {

    private List<List<S>> fronts;
    private int solutionsToSelect;
    /* 参考点 */
    private List<ReferencePoint<S>> referencePoints;
    private int numberOfObjectives;

    public EnvironmentalSelection(Builder<S> builder) {
        fronts = builder.getFronts();
        solutionsToSelect = builder.getSolutionsToSelet();
        referencePoints = builder.getReferencePoints();
        numberOfObjectives = builder.getNumberOfObjectives();
    }

    public EnvironmentalSelection(List<List<S>> fronts, int solutionsToSelect,
                                  List<ReferencePoint<S>> referencePoints, int numberOfObjectives) {
        this.fronts = fronts;
        this.solutionsToSelect = solutionsToSelect;
        this.referencePoints = referencePoints;
        this.numberOfObjectives = numberOfObjectives;
    }

    /**
     * 计算理想点，并将全部个体的原点移动到当前理想点.
     *
     * @return
     */
    private List<Double> translateObjectives() {

        List<Double> ideal_point = new ArrayList<>(numberOfObjectives);
        for (int f = 0; f < numberOfObjectives; f += 1) {
            double minf = Double.MAX_VALUE;
            for (int i = 0; i < fronts.get(0).size(); i += 1) {
                // min values must appear in the first front
                minf = Math.min(minf, fronts.get(0).get(i).objectives()[f]);
            }
            ideal_point.add(minf);
            /* 并将解空间远原点挪到理想点 */
            for (List<S> list : fronts) {
                for (S s : list) {
                    if (f == 0) {
                        // in the first objective we create the vector of conv_objs
                        setAttribute(s, new ArrayList<Double>());
                    }
                    getAttribute(s).add(s.objectives()[f] - minf);
                }
            }
        }
        return ideal_point;

    }

    // ----------------------------------------------------------------------
    // ASF: Achivement Scalarization Function
    // I implement here a effcient version of it, which only receives the index
    // of the objective which uses 1.0; the rest will use 0.00001. This is
    // different to the one impelemented in C++
    // ----------------------------------------------------------------------
    private double ASF(S s, int index) {
        double max_ratio = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < s.objectives().length; i++) {
            double weight = (index == i) ? 1.0 : 0.000001;
            max_ratio = Math.max(max_ratio, s.objectives()[i] / weight);
        }
        return max_ratio;
    }

    // ----------------------------------------------------------------------

    /**
     * 获取各个目标方向上的极值点.
     *
     * @return
     */
    private List<S> findExtremePoints() {
        /* 找出各个目标轴上的极值点，一个目标对应一个极值点 */
        List<S> extremePoints = new ArrayList<>();
        S min_indv = null;
        for (int f = 0; f < numberOfObjectives; f += 1) {
            double min_ASF = Double.MAX_VALUE;
            for (S s : fronts.get(0)) {
                // only consider the individuals in the first front
                /* f代表一个目标方向 */
                double asf = ASF(s, f);
                if (asf < min_ASF) {
                    min_ASF = asf;
                    min_indv = s;
                }
            }
            /* 在pareto前沿中找到极值点 */
            extremePoints.add(min_indv);
        }
        return extremePoints;
    }

    /* 高斯消元法 */
    public List<Double> guassianElimination(List<List<Double>> A, List<Double> b) {
        List<Double> x = new ArrayList<>();

        int N = A.size();
        for (int i = 0; i < N; i += 1) {
            A.get(i).add(b.get(i));
        }

        for (int base = 0; base < N - 1; base += 1) {
            for (int target = base + 1; target < N; target += 1) {
                double ratio = A.get(target).get(base) / A.get(base).get(base);
                for (int term = 0; term < A.get(base).size(); term += 1) {
                    A.get(target).set(term, A.get(target).get(term) - A.get(base).get(term) * ratio);
                }
            }
        }

        for (int i = 0; i < N; i++) {
            x.add(0.0);
        }

        for (int i = N - 1; i >= 0; i -= 1) {
            for (int known = i + 1; known < N; known += 1) {
                A.get(i).set(N, A.get(i).get(N) - A.get(i).get(known) * x.get(known));
            }
            x.set(i, A.get(i).get(N) / A.get(i).get(i));
        }
        return x;
    }

    /**
     * 构建超平面，通过获取各个方向上的截断距离构建超平面.
     *
     * @param extreme_points
     * @return
     */
    private List<Double> constructHyperplane(List<S> extreme_points) {
        // Check whether there are duplicate extreme points.
        // This might happen but the original paper does not mention how to deal with it.
        /**
         * 为什么会出现极值点重复的情况呢？原则上，以三个目标为例子，三个目标构成一个 xyz 空间，
         * 若是极值点就在原点呢？是不是就重复了,maybe.
         */
        boolean duplicate = false;
        for (int i = 0; !duplicate && i < extreme_points.size(); i += 1) {
            for (int j = i + 1; !duplicate && j < extreme_points.size(); j += 1) {
                duplicate = extreme_points.get(i).equals(extreme_points.get(j));
            }
        }
        /* 目标方向上的截距 */
        List<Double> intercepts = new ArrayList<>();
        if (duplicate) {
            /* 这种情况极少 */
            // cannot construct the unique hyperplane (this is a casual method to deal with the condition)
            for (int f = 0; f < numberOfObjectives; f += 1) {
                // extreme_points[f] stands for the individual with the largest value of objective f
                /* 注意这里有一个前后对应的关系，构建极值点集合构建的时候就按照object的index插入的，这里也是按照固定的对应顺序写入的。
                 * 但是ArrayList一定能保证取的顺序一致嘛？所以保险起见可使用LinkedList来进行存储.
                 */
                intercepts.add(extreme_points.get(f).objectives()[f]);
            }
        } else {
            /* 这里使用的是高斯消元法获取在各个目标上的截距离，一定目标的个数=极值点集合当前的实际大小，满足假设前提 */
            // Find the equation of the hyperplane
            List<Double> b = new ArrayList<>(); // (pop[0].objs().size(), 1.0);
            for (int i = 0; i < numberOfObjectives; i++) {
                b.add(1.0);
            }
            List<List<Double>> A = new ArrayList<>();
            for (S s : extreme_points) {
                List<Double> aux = new ArrayList<>();
                for (int i = 0; i < numberOfObjectives; i++) {
                    aux.add(s.objectives()[i]);
                }
                A.add(aux);
            }
            /**
             * 高斯消元基础：https://blog.csdn.net/u011815404/article/details/88890702
             * todo 应该还有优化空间，注意下内部实现.
             */
            List<Double> x = guassianElimination(A, b);
            // Find intercepts
            /* 获取对应反向上的截距 */
            for (int f = 0; f < numberOfObjectives; f += 1) {
                intercepts.add(1.0 / x.get(f));
            }
        }
        return intercepts;
    }

    /**
     * 目标正则化过程.
     * 10e-10 -->> 表示正无穷小.
     *
     * @param intercepts  截断距离
     * @param ideal_point 理想点
     */
    private void normalizeObjectives(List<Double> intercepts, List<Double> ideal_point) {
        for (int t = 0; t < fronts.size(); t += 1) {
            for (S s : fronts.get(t)) {
                /*{@link org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.EnvironmentalSelection.translateObjectives}*/
                /* 注意：solution#attributes的数值在translateObjectives方法内部已经完成源点到理想点的替换，即已经减去了理想点的坐标 */
                List<Double> conv_obj = (List<Double>) getAttribute(s); /* 这个应该放 for f 外面把？todo */
                for (int f = 0; f < numberOfObjectives; f++) {
                    if (Math.abs(intercepts.get(f) - ideal_point.get(f)) > 10e-10) {
                        conv_obj.set(f, conv_obj.get(f) / (intercepts.get(f) - ideal_point.get(f)));
                    } else {
                        /* 截断距离等于理想对应坐标 */
                        conv_obj.set(f, conv_obj.get(f) / (10e-10));
                    }
                }
            }
        }
    }

    /**
     * 计算正交距离.
     *
     * @param direction 参考点坐标
     * @param point     front解集前沿的个体
     * @return
     */
    private double perpendicularDistance(List<Double> direction, List<Double> point) {
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < direction.size(); i += 1) {
            numerator += direction.get(i) * point.get(i);
            denominator += Math.pow(direction.get(i), 2.0);
        }
        double k = numerator / denominator;

        double d = 0;
        for (int i = 0; i < direction.size(); i += 1) {
            d += Math.pow(k * direction.get(i) - point.get(i), 2.0);
        }
        /* 注意：这里需要开平方 */
        return Math.sqrt(d);
    }

    /**
     * 将解集前沿集合中的个体和参考点的联系起来.
     */
    private void associate() {

        for (int t = 0; t < fronts.size(); t++) {
            for (S s : fronts.get(t)) {
                int min_rp = -1;
                double min_dist = Double.MAX_VALUE;
                for (int referenceIndex = 0; referenceIndex < this.referencePoints.size(); ++referenceIndex) {
                    /* 获取距离 */
                    double distance = perpendicularDistance(
                            this.referencePoints.get(referenceIndex).position, (List<Double>) getAttribute(s));
                    if (distance < min_dist) {
                        min_dist = distance;
                        min_rp = referenceIndex;
                    }
                }
                if (t + 1 != fronts.size()) {
                    this.referencePoints.get(min_rp).AddMember();
                } else {
                    /* 参考点到最后一个支配分层的最近距离，实际计算的过程中，尤其在算法迭代的早期front中完全可能存在多个支配分层 */
                    this.referencePoints.get(min_rp).AddPotentialMember(s, min_dist);
                }
            }
        }

    }

    // ----------------------------------------------------------------------
    // SelectClusterMember():
    //
    // Select a potential member (an individual in the front Fl) and associate
    // it with the reference point.
    //
    // Check the last two paragraphs in Section IV-E in the original paper.
    // ----------------------------------------------------------------------
    S SelectClusterMember(ReferencePoint<S> rp) {
        S chosen = null;
        if (rp.HasPotentialMember()) {
            if (rp.MemberSize() == 0) {
                // currently has no member
                /* 选择距离当前参考点正交距离最短的个体 */
                chosen = rp.FindClosestMember();
            } else {
                /* 随机选一个 */
                chosen = rp.RandomMember();
            }
        }
        return chosen;
    }

    private TreeMap<Integer, ArrayList<ReferencePoint<S>>> referencePointsTree = new TreeMap<>();

    private void addToTree(ReferencePoint<S> rp) {
        var key = rp.MemberSize();
        if (!this.referencePointsTree.containsKey(key)) {
            this.referencePointsTree.put(key, new ArrayList<>());
        }
        this.referencePointsTree.get(key).add(rp);
    }

    @Override
    /* This method performs the environmental Selection indicated in the paper describing NSGAIII*/
    public List<S> execute(List<S> source) throws JMetalException {

        // The comments show the C++ code
        // ---------- Steps 9-10 in Algorithm 1 ----------
        if (source.size() == this.solutionsToSelect) {
            return source;
        }
        // ---------- Step 14 / Algorithm 2 ----------
        // vector<double> ideal_point = TranslateObjectives(&cur, fronts);
        /* 找到理想点，并将理想点替换成原坐标原点 */
        List<Double> ideal_point = translateObjectives();
        /* 找到各个目标方向上的极值点 */
        List<S> extreme_points = findExtremePoints();
        /* 获取各个目标方向上的截断距离，构建超平面 */
        List<Double> intercepts = constructHyperplane(extreme_points);
        /* front前沿解的正则化过程 */
        normalizeObjectives(intercepts, ideal_point);
        // ---------- Step 15 / Algorithm 3, Step 16 ----------
        /* 联系个体和参考点 */
        associate();
        /* 根据参考点关联的前沿中解的数量将参考点进行分组¬ */
        for (var referencePoint : this.referencePoints) {
            /* ReferencePoint#sort() 内部的实现是逆序的，从大到小*/
            referencePoint.sort();
            this.addToTree(referencePoint);
        }
        /* 获取随机器 */
        var rand = JMetalRandom.getInstance();
        List<S> result = new ArrayList<>();
        /* 开始填充结果集 */
        // ---------- Step 17 / Algorithm 4 ----------
        while (result.size() < this.solutionsToSelect) {
            /* 根据TreeMap的特性，这里优先出现的是参考点中关联点少的个体 -->> 这里就选择了低密度的个体所在的连续或离散解空间. */
            final var first = this.referencePointsTree.firstEntry().getValue();
            /* 随机获取一个解的index */
            final var min_rp_index = 1 == first.size() ? 0 : rand.nextInt(0, first.size() - 1);
            final var min_rp = first.remove(min_rp_index);
            if (first.isEmpty()) {
                this.referencePointsTree.pollFirstEntry();
            }
            S chosen = SelectClusterMember(min_rp);
            if (chosen != null) {
                /* 自增memberSize */
                min_rp.AddMember();
                /* 当加回到referencePointsTree的时候，min_rp对应的密度也相对升高，被高优的选择的级别类似降了一个级别 */
                this.addToTree(min_rp);
                /* 结果集合 */
                result.add(chosen);
            }
        }
        return result;

    }

    public static class Builder<S extends Solution<?>> {
        private List<List<S>> fronts;
        private int solutionsToSelect;
        private List<ReferencePoint<S>> referencePoints;
        private int numberOfObjctives;

        // the default constructor is generated by default

        public Builder<S> setSolutionsToSelect(int solutions) {
            solutionsToSelect = solutions;
            return this;
        }

        public Builder<S> setFronts(List<List<S>> f) {
            fronts = f;
            return this;
        }

        public int getSolutionsToSelet() {
            return this.solutionsToSelect;
        }

        public List<List<S>> getFronts() {
            return this.fronts;
        }

        public EnvironmentalSelection<S> build() {
            return new EnvironmentalSelection<>(this);
        }

        public List<ReferencePoint<S>> getReferencePoints() {
            return referencePoints;
        }

        public Builder<S> setReferencePoints(List<ReferencePoint<S>> referencePoints) {
            this.referencePoints = referencePoints;
            return this;
        }

        public Builder<S> setNumberOfObjectives(int n) {
            this.numberOfObjctives = n;
            return this;
        }

        public int getNumberOfObjectives() {
            return this.numberOfObjctives;
        }
    }

    @Override
    public void setAttribute(S solution, List<Double> value) {
        solution.attributes().put(getAttributeIdentifier(), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Double> getAttribute(S solution) {
        return (List<Double>) solution.attributes().get(getAttributeIdentifier());
    }

    @Override
    public Object getAttributeIdentifier() {
        return this.getClass();
    }

}
