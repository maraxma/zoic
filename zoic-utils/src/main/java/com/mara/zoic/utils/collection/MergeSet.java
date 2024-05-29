package com.mara.zoic.utils.collection;

import java.util.Collection;
import java.util.List;

/**
 * 并查集接口。
 * @param <T> 数据类型
 */
public interface MergeSet<T> {
    boolean isRelated(T t1, T t2);
    void setOrAddRelated(T t1, T t2);

    /**
     * 向并查集中添加一个元素，该元素与任何已存在的元素都不存在关联。
     * <p>如果已经存在该元素，则不产生任何影响。</p>
     * @param t 元素
     */
    void add(T t);

    /**
     * 从并查集中移除一个元素，若该元素存在和其他元素的关联，该关联都会被移除。
     * @param t 元素
     */
    void remove(T t);
    void addAll(Collection<T> coll);

    /**
     * 移除两个元素之间的关联。
     * <p>两个元素必须直接关联才可以移除关联，间接关联因为存在中间元素，关系不能破坏，因此间接关联不能被移除。</p>
     * <p>若元素不存在或者这两个元素不存在关系，则不做操作。</p>
     * @param t1 元素1
     * @param t2 元素2
     */
    void removeRelated(T t1, T t2);

    T getParent(T t);
    T getRoot(T t);
    boolean isEmpty();
    int size();
    int groupCount();
    List<T> getGroupLeaders();
}
