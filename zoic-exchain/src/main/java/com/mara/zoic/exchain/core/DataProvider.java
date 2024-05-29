package com.mara.zoic.exchain.core;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * 数据提供器。“数据提供器”主要用于输出数据，输出的数据既可以是设定好的即时数据，也可以是通过各种手段延迟加载的数据。
 * @author Mara.X.Ma
 * @since 1.0.0 2021-07-21
 * @param <T> 需要生成的数据的类型
 */
public interface DataProvider<T> extends Supplier<T>, Serializable, Cloneable {

    /**
     * 获得此数据提供器的数据。
     * @return 数据
     */
    @Override
    T get();

    /**
     * 重置数据加提供器。重置后内部缓存（如果有的话）会被清空，且可以再次通过工厂函数生成数据。
     * <p>在实际应用中，此方法可以用来实现重新加载或运算数据。
     */
    void reset();

}
