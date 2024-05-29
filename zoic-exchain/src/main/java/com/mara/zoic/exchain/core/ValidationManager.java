package com.mara.zoic.exchain.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * 校验流程管理器。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-31
 */
public interface ValidationManager {
    
    /**
     * 向此管理器注册一个校验流程。
     * @param validationName 校验名称
     * @param validation 校验
     */
    void register(String validationName, Validation validation);

    /**
     * 取消注册一个校验流程。
     * @param validationName 校验名称
     */
    void deregister(String validationName);
    
    /**
     * 开始校验一批数据。运行在 {@link ValidationMode#getDefault()} 模式下。
     * @param <T> 数据类型
     * @param validationName 校验名称
     * @param data 数据
     * @param executorService 执行器，如果校验流程中存在并行节点，那么需要一个线程池来执行
     * @return 校验结果
     */
    <T> Map<T, ValidationResult<T>> validate(String validationName, Set<T> data, ExecutorService executorService);

    /**
     * 开始校验一批数据。
     * @param <T> 数据类型
     * @param validationName 校验名称
     * @param data 数据
     * @param executorService 执行器，如果校验流程中存在并行节点，那么需要一个线程池来执行
     * @param validationMode 校验模式，可以是null，如果是null则运行在默认模式下
     * @param stopMessageType 如果validationMode是IMMEDIATE_STOP_XX，在哪种MessageType下触发停止。可以是null，如果是null则采用默认的{@link org.mosin.bocus.core.vd.ValidationMessage.MessageType#ERROR}。
     *                        如果validationMode是SUSTAINABLE，此参数的设定不具有意义
     * @return 校验结果
     */
    <T> Map<T, ValidationResult<T>> validate(String validationName, Set<T> data, ExecutorService executorService, ValidationMode validationMode, ValidationMessage.MessageType stopMessageType);

    /**
     * 开始校验，校验中如果遇到并行节点，那么采用ForkJoin方式校验。运行在 {@link ValidationMode#getDefault()} 模式下。
     * @param <T> 数据类型
     * @param validationName 验证名称
     * @param data 数据
     * @param forkJoinPool 执行器线程池
     * @return 校验结果
     */
    <T> Map<T, ValidationResult<T>> validateForkJoin(String validationName, Set<T> data, ForkJoinPool forkJoinPool);

    /**
     * 开始校验，校验中如果遇到并行节点，那么采用ForkJoin方式校验。
     * @param <T> 数据类型
     * @param validationName 验证名称
     * @param data 数据
     * @param forkJoinPool 执行器线程池
     * @param validationMode 校验模式，可以是null，如果是null则运行在默认模式下
     * @param stopMessageType 如果validationMode是IMMEDIATE_STOP_XX，在哪种MessageType下触发停止。可以是null，如果是null则采用默认的{@link org.mosin.bocus.core.vd.ValidationMessage.MessageType#ERROR}。
     *                        如果validationMode是SUSTAINABLE，此参数的设定不具有意义
     * @return 校验结果
     */
    <T> Map<T, ValidationResult<T>>  validateForkJoin(String validationName, Set<T> data, ForkJoinPool forkJoinPool, ValidationMode validationMode, ValidationMessage.MessageType stopMessageType);

    /**
     * 开始校验。运行在 {@link ValidationMode#getDefault()} 模式下。
     * <p>此方法相对于 {@link #validateForkJoin(String, Set, ForkJoinPool)} 和 {@link #validate(String, Set, ExecutorService)} 方法
     * 少了 ExecutorService 和 ForkJoinPool，因此此方法仅支持不含有并行节点的Validation。如果含有并行节点，那么会抛出异常并提示使用此方法的其他重载方法。
     * @param validationName 校验名称
     * @param data 数据
     * @param <T> 数据类型
     * @return 校验结果
     */
    <T> Map<T, ValidationResult<T>> validate(String validationName, Set<T> data);

    /**
     * 开始校验。
     * <p>此方法相对于 {@link #validateForkJoin(String, Set, ForkJoinPool)} 和 {@link #validate(String, Set, ExecutorService)} 方法
     * 少了 ExecutorService 和 ForkJoinPool，因此此方法仅支持不含有并行节点的Validation。如果含有并行节点，那么会抛出异常并提示使用此方法的其他重载方法。
     * @param validationName 校验名称
     * @param data 数据
     * @param validationMode 校验模式，可以是null，如果是null则运行在默认模式下
     * @param stopMessageType 如果validationMode是IMMEDIATE_STOP_XX，在哪种MessageType下触发停止。可以是null，如果是null则采用默认的{@link org.mosin.bocus.core.vd.ValidationMessage.MessageType#ERROR}。
     *                        如果validationMode是SUSTAINABLE，此参数的设定不具有意义
     * @param <T> 数据类型
     * @return 校验结果
     */
    <T> Map<T, ValidationResult<T>> validate(String validationName, Set<T> data, ValidationMode validationMode, ValidationMessage.MessageType stopMessageType);
    
    /**
     * 关闭一个校验流程中某个校验器。
     * @param validationName 校验名称
     * @param validatorName 校验器名称
     */
    void disableValidator(String validationName, String validatorName);

    /**
     * 开启一个校验流程中某个校验器。
     * @param validationName 校验名称
     * @param validatorName 校验器名称
     */
    void enableValidator(String validationName, String validatorName);

    /**
     * 通过名称获得一个校验流程。
     * <p>仅针对已经注册的校验流程。
     * @param validationName 校验名称
     * @return 校验流程
     */
    Validation getValidation(String validationName);
}
