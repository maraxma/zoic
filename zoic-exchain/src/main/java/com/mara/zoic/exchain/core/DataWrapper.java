package com.mara.zoic.exchain.core;


import java.util.Map;
import java.util.Set;

import com.mara.zoic.exchain.core.ValidationMessage.MessageType;

/**
 * 数据包装器代表一个经过包装的数据，里面提供了一些额外的状态参数用于控制流程。
 * <p>用户可以在此包装器上传递数据，也可以通过它来获知上下文的一些状态，还可以通过它直接汇报消息。
 * @author Mara.X.Ma
 * @since 1.0.0 2021-09-25
 */
public interface DataWrapper<T> {
    /**
     * 获得被包装的原始数据。
     * @return 原始数据
     */
    T getOriginalData();

    /**
     * 设定是否忽略接下来的所有校验器（仅针对此数据）。
     * @param ignore 是否忽略，true-是；false-否
     */
    void setIgnoreAllFollowingValidators(boolean ignore);

    /**
     * 是否忽略接下来的所有校验器（仅针对此数据）。
     * <p>默认情况下，所有的数据该状态为false。</p>
     * @return true-是；false-否
     * @see #setIgnoreAllFollowingValidators(boolean)
     */
    boolean isIgnoreAllFollowingValidators();

    /**
     * 设定需要忽略的校验器（仅针对此数据）。
     * @param validatorNames 验证器名称，可以是正则表达式，用于匹配名称，正则表达式需要包含在"/"中，如"/Validator\d/"
     */
    void setIgnoreValidators(String... validatorNames);

    /**
     * 添加需要忽略的校验器（仅针对此数据）。
     * @param validatorNames 验证器名称，可以是正则表达式，用于匹配名称，正则表达式需要包含在"/"中，如"/Validator\d/"
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
     * 获得已设定的需要忽略的验证器（仅针对此数据）。请将返回的名称视为正则表达式。
     * @return 已设定的需要忽略的验证器，未设定或默认情况下，这里返可返回null或者是空集合
     */
    Set<String> getIgnoredValidators();

    /**
     * 获得即将到来的验证器。
     * <p>注意：这个方法返回的是一个瞬时快照，一般不具有很大的参考性，在只有串行校验的情况下具有较高的参考性。</p>
     * <p>如果当前节点节点是并行节点中的一员，那么他的FutureValidators包含其并行的兄弟节点；如果当前节点是处于深度并行流程中的，那么所有可能与之并行的节点都将被列出。
     * @return 即将到来的验证器集合。可能是null或者空集合
     */
    Set<String> getFutureValidators();

    /**
     * 获得在该数据上已经产生的验证信息。
     * <p>注意：这个方法返回的是一个瞬时快照，一般不具有很大的参考性，在只有串行校验的情况下具有较高的参考性。</p>
     * @return 已经产生的验证信息
     */
    ValidationResult<T> getValidatedInfo();

    /**
     * 向上下文汇报关于此数据的校验消息。
     * @param messageType 消息类型，不能为null
     * @param message 消息，不能为null
     * @param code 代码，可以是null
     * @param extra 额外信息，可以是null
     * @param cause 导致此消息的可抛出对象，可以是null
     */
    default void report(MessageType messageType, String message, String code, Map<String, Object> extra, Throwable cause) {
        ValidationContexts.autoDetect().report(getOriginalData(), messageType, message, code, extra, cause);
    }
    
    /**
     * 向上下文汇报关于此数据的校验消息。
     * @param messageType 消息类型，不能为null
     * @param message 消息，不能为null
     * @param code 代码，可以是null
     * @param extra 额外信息，可以是null
     */
    default void report(MessageType messageType, String message, String code, Map<String, Object> extra) {
        report(messageType, message, code, extra, null);
    }
    
    /**
     * 向上下文汇报关于此数据的校验消息。
     * @param messageType 消息类型，不能为null
     * @param message 消息，不能为null
     * @param code 代码，可以是null
     */
    default void report(MessageType messageType, String message, String code) {
        report(messageType, message, code, null, null);
    }
    
    /**
     * 向上下文汇报关于此数据的校验消息。
     * @param messageType 消息类型，不能为null
     * @param message 消息，不能为null
     */
    default void report(MessageType messageType, String message) {
        report(messageType, message, null, null, null);
    }
    
    /**
     * 向上下文汇报关于此数据的校验消息（INFO级别）。
     * @param message 消息，不能为null
     */
    default void reportInfo(String message) {
        report(MessageType.INFO, message, null, null, null);
    }
    
    /**
     * 向上下文汇报关于此数据的校验消息（DEBUG级别）。
     * @param message 消息，不能为null
     */
    default void reportDebug(String message) {
        report(MessageType.DEBUG, message, null, null, null);
    }
    
    /**
     * 向上下文汇报关于此数据的校验消息（WARNING级别）。
     * @param message 消息，不能为null
     */
    default void reportWarning(String message) {
        report(MessageType.WARNING, message, null, null, null);
    }
    
    /**
     * 向上下文汇报关于此数据的校验消息（ERROR级别）。
     * @param message 消息，不能为null
     */
    default void reportError(String message) {
        report(MessageType.ERROR, message, null, null, null);
    }
    
    /**
     * 向上下文汇报关于此数据的校验消息（Fatal级别）。
     * @param message 消息，不能为null
     */
    default void reportFatal(String message) {
        report(MessageType.FATAL, message, null, null, null);
    }
}
