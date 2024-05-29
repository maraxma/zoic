package com.mara.zoic.exchain.core;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 代表一个针对校验结果的条件对象。
 * @param <T> 被校验的数据类型
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-26
 */
public interface ValidationResultCondition<T> {

    void then(Consumer<ValidationResult<T>> action);

    <R> R thenReturn(Function<ValidationResult<T>, R> operation);

    <R> R thenReturn(Function<ValidationResult<T>, R> operation, R notMatchReturn);

    void thenThrowRuntimeException(Function<ValidationResult<T>, ? extends RuntimeException> exceptionGenerator);

    default void thenThrowRuntimeException(String message) {
        thenThrowRuntimeException(vr -> new RuntimeException(message));
    }
}
