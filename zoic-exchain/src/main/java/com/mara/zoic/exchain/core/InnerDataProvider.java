package com.mara.zoic.exchain.core;

import java.util.function.Supplier;

/**
 * 数据提供器的默认内部实现。
 *
 * @param <T> 数据类型
 * @author Mara.X.Ma
 * @since 1.0.0 2021-07-21
 */
final class InnerDataProvider<T> implements DataProvider<T> {

    private static final long serialVersionUID = 3201624436524786915L;

    private Supplier<T> dataGenerator;

    /** cache是在构造函数中设定的，后面不会变更，可以不用做可见性 */
    private boolean cache = false;

    /** loaded是在运行时被改变的，并且get操作需要借鉴它的值，因此必须修饰可见性 */
    private volatile boolean loaded = false;

    /** data可以不做可见性，因为它的值要么是在构造函数中定义，要么是由loaded来控制 */
    private T data = null;

    public InnerDataProvider(T data) {
        this.data = data;
        this.loaded = true;
        this.cache = true;
    }

    public InnerDataProvider(Supplier<T> dataGenerator, boolean cache, boolean generateNow) {
        this.dataGenerator = dataGenerator;
        this.cache = cache;
        if (cache && generateNow) {
            // 注意，当cache = false时，generateNow无意义，因为每次都需要及时生成
            data = dataGenerator.get();
            loaded = true;
        }
    }

    @Override
    public T get() {
        if (cache) {
            if (!loaded) {
                synchronized (this) {
                    if (!loaded) {
                        data = dataGenerator.get();
                        loaded = true;
                    }
                }
            }
            return data;
        } else {
            // reset方法对无缓存的操作无影响，因此这里不用同步
            // 在并发环境下每次都生成新的数据返回，符合预期
            return dataGenerator.get();
        }
    }

    @Override
    public void reset() {
        data = null;
        if (cache) {
            if (!loaded) {
                synchronized (this) {
                    if (!loaded) {
                        loaded = false;
                    }
                }
            }
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}