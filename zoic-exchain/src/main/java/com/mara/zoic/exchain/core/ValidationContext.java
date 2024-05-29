package com.mara.zoic.exchain.core;

import java.util.Set;

/**
 * 验证上下文。
 * <p>验证上下文对象在一次完整的validation中只存在唯一的一个。
 * <p>验证上下文也是重要的流程控制器，可以控制在校验链中的一些行为（比如忽略一些验证器）。
 * <p>验证上下文对象将在验证过程中提供一些额外的上下文参数。用户也可以通过它来传递在整个验证流程中生效的数据。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-29
 * @see AbstractValidator
 * @see Validation
 * @see DataProvider
 */
public interface ValidationContext extends ValidationReportable {

    /**
     * 通过指定的key获得一个数据提供器对象（可以是内置的，也可以是用户的）。
     * @param key key
     * @return 数据提供器
     */
    DataProvider<?> get(String key);

    /**
     * 向context中存放一组用户数据。存放的数据在整个Validation流程中有效。
     * @param key key
     * @param dataProvider 数据提供器
     * @return context本身，可用于链式调用
     */
    ValidationContext put(String key, DataProvider<?> dataProvider);

    /**
     * 向context中删除一组用户数据。
     * @param key key
     * @return context本身，可用于链式调用
     */
    ValidationContext remove(String key);

    /**
     * 移除所有用户数据
     * @return context本身，可用于链式调用
     */
    ValidationContext clear();

    /**
     * 获得验证名称。
     * @return 验证名称
     */
    String getValidationName();

    /**
     * 获得当前验证器的名称。
     * @return 验证器名称
     */
    String getValidatorName();

    /**
     * 获得当前验证器的唯一实例。
     * @return 唯一实例
     */
    AbstractValidator<?> getValidator();

    /**
     * 检查取消点。如果有被设定为取消，那么将会抛出 {@link java.util.concurrent.CancellationException}。
     * <p>请不要手动捕获此方法抛出的{@link java.util.concurrent.CancellationException}，否则将导致并行验证器取消功能失效。
     */
    void checkCancellation();

    /**
     * 取消其他并行的验证器的验证（串行验证器不受影响）。
     */
    void cancelOtherParallelValidators();

    /**
     * 是否已经检查过取消点。
     * @return ture代表是，false代表否
     */
    boolean isCancellationChecked();

    /**
     * 忽略后续所有校验器（默认是false）。
     * @param ignore 是够忽略，true代表是，false发表否
     */
    void ignoreAllFollowingValidators(boolean ignore);

    /**
     * 获得是否已经设定了忽略后续所有校验器（默认会返回false）。
     * @return true代表是，false代表不是
     */
    boolean isIgnoreAllFollowingValidators();

    /**
     * 获得所有已经设定的需要忽略的校验器（默认是空集合）。
     * @return 校验器名称集合（只读）
     */
    Set<String> getIgnoredValidators();

    /**
     * 设定需要忽略的校验器。
     * @param validatorNames 校验器名称，支持正则表达式
     */
    void setIgnoreValidators(String... validatorNames);

    /**
     * 设添加需要忽略的校验器。
     * @param validatorNames 校验器名称，支持正则表达式
     */
    void addIgnoreValidators(String... validatorNames);

    /**
     * 从已忽略列表中移除指定的验证器名称。
     * @param validatorNames 验证器名称，如果有正则表达式存在，那么需要精确匹配该表达式才能移除
     */
    void removeIgnoredValidators(String... validatorNames);

    /**
     * 清除已设定的验证器忽略列表。
     */
    void clearIgnoredValidators();

    /**
     * 获得 {@link ValidationManager} 实例。
     * @return {@link ValidationManager} 实例
     */
    ValidationManager getValidationManager();

    /**
     * 获得 {@link Validation} 实例。
     * @return {@link Validation} 实例。
     */
    Validation getValidation();

    /**
     * 获得校验模式。
     * @return 校验模式
     */
    ValidationMode getValidationMode();

    /**
     * 获得在即停模式下的消息类型。
     * @return 消息类型。当在急停模式下，且消息类型等于或者严重于此messageType时校验会中止
     */
    ValidationMessage.MessageType getStopMessageType();
}
