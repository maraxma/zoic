package com.mara.zoic.exchain.core;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 单标针对单一数据的校验结果。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-29
 * @param <T> 数据类型
 */
public interface ValidationResult<T> {

    /**
     * 获得被校验的数据
     * @return 原数据
     */
    T getOriginalData();
    
    /**
     * 获得该结果是由哪个验证产生的。
     * @return 验证名称
     */
    String getValidationName();

    /**
     * 获得针对该数据的全部校验结果信息。
     * @return 校验结果信息
     */
    Set<ValidationMessage> getMessages();

    /**
     * 使用指定的条件将该验证结果转换为可操作对象，方便快速操作。
     * @param condition 条件
     * @return 可操作条件对象
     */
    ValidationResultCondition<T> when(Function<ValidationResult<T>, Boolean> condition);

    /**
     * 判断消息是否为空
     * @return true代表是，false代表否
     */
    default boolean isMessageEmpty() {
        return getMessages().isEmpty();
    }

    /**
     * 是否包含指定的消息。
     * @param message 消息
     * @return true代表包含，false代表不包含
     */
    default boolean containsMessage(String message) {
        return findMessage(msg -> Objects.equals(message, msg.getMessage())) != null;
    }

    /**
     * 是否包含指定的消息片段。
     * @param messageSegment 消息片段
     * @return true代表包含，false代表不包含
     */
    default boolean containsMessageSegment(String messageSegment) {
        if (messageSegment == null) {
            return false;
        }
        return findMessage(msg -> msg.getMessage().contains(messageSegment)) != null;
    }

    /**
     * 是否包含指定的针对消息的正则表达式。
     * @param messageRegex 消息正则表达式
     * @return true代表包含，false代表不包含
     */
    default boolean containsMessageRegex(String messageRegex) {
        if (messageRegex == null) {
            return false;
        }
        return findMessage(msg -> msg.getMessage().matches(messageRegex)) != null;
    }

    /**
     * 是否包含指定的消息代码。
     * @param code 消息代码
     * @return true代表包含，false代表不包含
     */
    default boolean containsCode(String code) {
        return findMessage(msg -> Objects.equals(code, msg.getCode())) != null;
    }

    /**
     * 是否包含指定的消息类型。
     * @param type 消息类型
     * @return true代表包含，false代表不包含
     */
    default boolean containsType(ValidationMessage.MessageType type) {
        return findMessage(msg -> Objects.equals(type, msg.getMessageType())) != null;
    }

    /**
     * 是否包含指定的验证器名称。
     * @param validatorName 验证器名称
     * @return true代表包含，false代表不包含
     */
    default boolean containsValidatorName(String validatorName) {
        return findMessage(msg -> Objects.equals(validatorName, msg.getValidatorName())) != null;
    }
    /**
     * 是否包含指定的验证器名称正则表达式。
     * @param validatorNameRegex 验证器名称正则表达式
     * @return true代表包含，false代表不包含
     */
    default boolean containsValidatorNameRegex(String validatorNameRegex) {
        if (validatorNameRegex == null) {
            return false;
        }
        return findMessage(msg -> msg.getValidatorName().matches(validatorNameRegex)) != null;
    }


    /**
     * 是否包含指定的验证器类。
     * @param validatorClass 验证器类
     * @return true代表包含，false代表不包含
     */
    default boolean containsValidatorClass(Class<? extends AbstractValidator<?>> validatorClass) {
        return findMessage(msg -> Objects.equals(validatorClass, msg.getValidatorClass())) != null;
    }

    /**
     * 通过指定的条件获得遇到的第一个Message。如果不存在任何Message，那么返回null。
     * @param predicate 条件
     * @return null或者是符合条件的第一个Message
     */
    default ValidationMessage findMessage(Predicate<ValidationMessage> predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("predicate cannot be null");
        }
        Set<ValidationMessage> messages = getMessages();
        for (ValidationMessage msg : messages) {
            if (predicate.test(msg)) {
                return msg;
            }
        }
        return null;
    }

    /**
     * 通过指定的条件查找所有符合的Message。如果不存在任何Message，那么返回空集合。
     * @param predicate 条件
     * @return 包含所有符合条件的集合，可能是空集合
     */
    default Set<ValidationMessage> findMessages(Predicate<ValidationMessage> predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("predicate cannot be null");
        }
        return getMessages().stream().filter(predicate).collect(Collectors.toSet());
    }

    /**
     * 获得存在的第一个Message，如果不存在任何Message，那么返回null。
     * @return null或者存在的第一个Message
     */
    default ValidationMessage findFirstMessage() {
        return getMessages().stream().findFirst().orElse(null);
    }

    /**
     * 获得任意一个Message，如果不存在任何Message，那么返回null。
     * @return null或者存在的任意一个Message
     */
    default ValidationMessage findAnyMessage() {
        return getMessages().stream().findAny().orElse(null);
    }
}
