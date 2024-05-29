package com.mara.zoic.exchain.core;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证器管理器。用于管理所有的Validator实例。
 * <p>基于享元模式，所有的validator只产生一个实例。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-31
 */
class ValidatorManager {
    
    private static final ConcurrentHashMap<Class<? extends AbstractValidator<?>>, AbstractValidator<?>> VALIDATOR_INSTANCES = new ConcurrentHashMap<>(10);

    /**
     * 从缓存中获取或者创建一个Validator实例。如果创建，那么是通过反射创建的。
     * @param <T> 验证器的目标数据类型
     * @param validatorClass 验证器类
     * @return 验证器实例
     */
    @SuppressWarnings("unchecked")
    static <T> AbstractValidator<T> getOrCreate(Class<? extends AbstractValidator<?>> validatorClass) {
        return (AbstractValidator<T>) VALIDATOR_INSTANCES.compute(validatorClass, (k, v) -> {
            try {
                return v == null ? validatorClass.getDeclaredConstructor(new Class<?>[]{}).newInstance() : v;
            } catch (Exception e) {
                throw new RuntimeException("Cannot get or create validator for class: " + validatorClass.getName(), e);
            }
        });
    }

    /**
     * 从缓存中删除一个验证器实例。如果再次需要此验证器，那么 {@link #getOrCreate(Class)} 方法将会再次创建。
     * @param validatorClass 验证器类
     */
    static void remove(Class<? extends AbstractValidator<?>> validatorClass) {
        VALIDATOR_INSTANCES.remove(validatorClass);
    }
    
}
