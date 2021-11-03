package org.uma.jmetal.util.naming;

/**
 * A {@link DescribedEntity} is identified through its name ({@link #getName()})
 * and further detailed through its description ({@link #getDescription()}).
 *
 * @author Matthieu Vergne <matthieu.vergne@gmail.com>
 */
/* 这个就是一个算法描述类，所以只要是实现了{@see org.uma.jmetal.algorithm.Algorithm} 接口算法类都需要实现对应的接口功能 */
public interface DescribedEntity {
    /**
     * @return the name of the {@link DescribedEntity}
     */
    public String getName();

    /**
     * @return the description of the {@link DescribedEntity}
     */
    public String getDescription();
}
