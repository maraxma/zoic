package com.mara.zoic.utils.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * {@link Unsafe} 帮助类，用于帮助用户获得 {@link Unsafe} 实例。
 * <p>{@link Unsafe} 不是对开发者开放的API。此类将通过反射机制获得 {@link Unsafe} 的实例。慎用 {@link Unsafe}，在必要时候或者你充分了解它的前提下才使用它。</p>
 * <p style="color:red">在Java1.9以后推荐使用 {@link java.lang.invoke.VarHandle} 类来替代 {@link Unsafe}，前者具有更高的安全性以及更好的性能。
 * 在Java1.9后 {@link java.lang.invoke.VarHandle} 已经替代了绝大t部分 J.U.C 包下的工具类中的 Unsafe 引用。</p>
 * @author mm92
 * @since 1.0.0
 * @see Unsafe
 * @see java.lang.invoke.VarHandle
 */
public final class UnsafeHelper {

	private static volatile Unsafe unsafe;

    /**
     * 获得{@link Unsafe}的实例。
     * <p>此方法通过反射机制绕开了{@link Unsafe#getUnsafe()}方法的限制
     * （原方法会检查调用者类的类加载器，如果不是Bootstrap类加载器则会抛出{@link SecurityException}异常）。</p>
     * @return {@link Unsafe}实例
     */
    public static Unsafe getUnsafe() {
        if (null == unsafe) {
            synchronized (UnsafeHelper.class) {
                if (null == unsafe) {
                    Field theUnsafeInstance = null;
                    try {
                        theUnsafeInstance = Unsafe.class.getDeclaredField("theUnsafe");
                        theUnsafeInstance.setAccessible(true);
                        unsafe = (Unsafe) theUnsafeInstance.get(Unsafe.class);
                    } catch (Exception e) {
                        throw new IllegalStateException("Cannot acquire Unsafe instance", e);
                    }
                }
            }
        }
        return unsafe;
    }
}
