package com.mara.zoic.exchain.core;

import java.util.function.Supplier;

/**
 * {@link DataProvider} 工厂类。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-29
 */
public class DataProviders {
    /**
     * 构造使用给定的及时数据生成数据的数据提供器实例。在数据提供器上使用 {@link DataProvider#get()} 方法获得此数据。
     * <p>在生成数据提供器实例后内部的数据将是不再可更换的。且，此数据会被缓存。
     * <p>使用此方法生成的数据提供器不同于 {@link #factory(Supplier, boolean, boolean)} 生成的，后者可以在 {@link DataProvider#get()} 后继续
     * {@link DataProvider#get()}，而前者 {@link DataProvider#get()} 后会数据被永久置为null，再次 {@link DataProvider#get()} 也将只能返回null。
     * <p>此方法等同于 factory(() -> data, true, true)。但是此方法具有较高的效率，因为并不会涉及到函数式接口（函数式接口会生成匿名对象）。
     * 如果你只需要简单地输出不需要计算的常量数据，请优先考虑此方法。
     * @param <T> 数据类型
     * @param data 数据
     * @return 数据提供器实例
     * @see DataProvider#get()
     * @see #factory(Supplier, boolean, boolean)
     * @see #factoryLazyCache(Supplier) 
     */
    public static <T> DataProvider<T> immediate(T data) {
        return new InnerDataProvider<>(data);
    }

    /**
     * 构造使用工厂方法生成数据的数据提供器实例。
     * <p>懒加载数据且不多次运算加载可使用 factory(dataGenerator, true, false)来实现。
     * @param <T> 数据类型
     * @param dataGenerator 数据生成器
     * @param cache 是否将生成的数据缓存起来，为 {@code true} 时即会缓存，第二次调用 {@link DataProvider#get()} 方法会立即返回缓存中的数据
     * @param generateNow 是否立即生成数据，为 {@code true} 时会立即调用 dataGenerator 生成数据，此参数在cache = false时无效，
     *                    因为没有意义
     * @return 数据提供器实例
     * @see DataProvider#get()
     * @see #immediate(Object)
     * @see #factoryLazyCache(Supplier)
     */
    public static <T> DataProvider<T> factory(Supplier<T> dataGenerator, boolean cache, boolean generateNow) {
        return new InnerDataProvider<>(dataGenerator, cache, generateNow);
    }

    /**
     * 构造使用工厂方法生成数据的数据提供器实例。采用懒加载并且启用缓存，效果等同于factory(dataGenerator, true, false)。
     * @param <T> 数据类型
     * @param dataGenerator 数据生成器
     * @return 数据提供器实例
     * @see DataProvider#get()
     * @see #immediate(Object)
     * @see #factory(Supplier, boolean, boolean) 
     */
    public static <T> DataProvider<T> factoryLazyCache(Supplier<T> dataGenerator) {
        return new InnerDataProvider<>(dataGenerator, true, false);
    }
}
