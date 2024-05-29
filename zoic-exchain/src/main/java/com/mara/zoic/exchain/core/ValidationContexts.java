package com.mara.zoic.exchain.core;

import com.mara.zoic.exchain.exception.NotValidationEnvironmentException;

/**
 * 验证上下文工具类。可以帮助快速获取验证上下文。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-31
 */
public class ValidationContexts {
    /**
     * 自动从环境中检测ValidationContext对象。
     * @return ValidationContext对象
     * @throws NotValidationEnvironmentException 当不是处于校验环境下时抛出
     */
    public static ValidationContext autoDetect() throws NotValidationEnvironmentException {
        ValidationContext validationContext = ValidationContextHolder.HOLDER.get();
        if (validationContext == null) {
            throw new NotValidationEnvironmentException("Not in validation environment currently, cannot acquire ValidationContext");
        }
        return validationContext;
    }
}
