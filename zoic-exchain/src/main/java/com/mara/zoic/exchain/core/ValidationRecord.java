package com.mara.zoic.exchain.core;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 代表一个验证的相关记录。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-29
 */
public interface ValidationRecord<T> {
    /**
     * 获得数据在该验证器上的开始校验的时间点。
     * @return 开始时间点
     */
    ZonedDateTime getStartDateTime();
    
    /**
     * 获得数据在该验证器上的结束时间点。
     * @return 结束时间点
     */
    ZonedDateTime getEndDateTime();
    
    /**
     * 获得验证名称。
     * @return 验证名称
     */
    String getValidationName();
    
    /**
     * 获得验证器名称。
     * @return 验证器名称
     */
    String getValidatorName();
    
    /**
     * 获得验证器类。
     * @return 验证器类
     */
    Class<? extends AbstractValidator<?>> getValidatorClass();

    /**
     * 获得数据经过该验证器的持续时长。
     * @return 持续时长
     */
    Duration getDuration();
    
    /**
     * 获得被校验的原始数据。
     * <p>注意，返回的原始数据Set不会被包装，因此，该Set可能是可修改的。
     * @return 原始数据
     */
    Set<T> getOriginalData();
    
    /**
     * 获得验证结果。
     * @return 验证结果。Map的Key是原始数据，Value是关于该数据的验证消息。
     */
    Map<T, ValidationResult<T>> getValidationResults();
}
