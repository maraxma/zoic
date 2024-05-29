package com.mara.zoic.exchain.core;

import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 代表验证后出示的消息。
 */
class DefaultValidationMessage implements ValidationMessage {
    
    /**
     * 消息类型
     */
    MessageType messageType = MessageType.INFO;
    
    /**
     * 消息，代表此验证的具体汇报信息
     */
    final String message;

    /**
     * 消息代码，开发习惯中一般在校验结果中会包含，可以留空
     */
    String code;

    /**
     * 描述问题出现在哪个字段，可以留空
     */
    String field;

    /**
     * 描述导致此消息的异常，可以是null
     */
    Throwable cause;

    /**
     * 附加的额外信息，在这儿可以自定义需要附加的信息，可以留空
     */
    Map<String, Object> extra = new LinkedHashMap<>();

    /*
     * 如下的字段可以提供一些额外信息，在某些场景可能会用到
     * 如下的字段由系统填充
     */

    /**
     * 描述产生此验证结果的验证名称。
     */
    String validationName;

    /**
     * 描述给出验证结果的验证器的名称，由程序自行附加的，一定会存在
     */
    String validatorName;

    /**
     * 描述报告这个验证结果验证器类，由程序自行附加的，一定会存在
     */
    Class<? extends AbstractValidator<?>> validatorClass;

    /**
     * 描述汇报此消息的代码行号，由程序自行附加的，一定会存在
     */
    int reportLineNumber;

    /**
     * 描述验证器可接受的类（即定义在AbstractDataValidator上的具体被验证类型），由程序自行附加的，可能是null
     */
    Class<?> validatorAcceptableClass;
    
    /**
     * 描述报告这个验证结果的堆栈行，由程序自行附加的，可能是null
     */
    StackTraceElement reportCallerTrace;

    /**
     * 描述使用这个验证器的堆栈行，由程序自行附加的，可能是null
     */
    StackTraceElement validatorCallerTrace;
    
    /**
     * 记录产生此报告的时间
     */
    ZonedDateTime reportTime;

    /**
     * 记录产生此报告的线程名称
     */
    String reportThreadName;

    DefaultValidationMessage(String message) {
        this.message = message;
    }

    public @NotNull String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    public Throwable getCause() {
        return cause;
    }

    public Map<String, Object> getExtra() {
        return Collections.unmodifiableMap(extra);
    }

    @Override
    public @NotNull String getValidationName() {
        return validationName;
    }

    public @NotNull String getValidatorName() {
        return validatorName;
    }

    public Class<?> getValidatorAcceptableClass() {
        return validatorAcceptableClass;
    }

    public @NotNull Class<? extends AbstractValidator<?>> getValidatorClass() {
        return validatorClass;
    }

    @Override
    public @NotNull MessageType getMessageType() {
        return messageType;
    }

    @Override
    public @NotNull ZonedDateTime getReportDateTime() {
        return reportTime;
    }

    @Override
    public int getReportLineNumber() {
        return reportLineNumber;
    }

    @Override
    public StackTraceElement getReportCallerTrace() {
        return reportCallerTrace;
    }

    @Override
    public StackTraceElement getValidatorCallerTrace() {
        return validatorCallerTrace;
    }
    
    @Override
    public String toString() {
        return messageType + ": " + message;
    }

    @Override
    public @NotNull String getReportThreadName() {
        return reportThreadName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultValidationMessage that = (DefaultValidationMessage) o;
        return reportLineNumber == that.reportLineNumber && messageType == that.messageType && Objects.equals(message, that.message) && Objects.equals(validationName, that.validationName) && Objects.equals(validatorName, that.validatorName) && Objects.equals(validatorClass, that.validatorClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType, message, validationName, validatorName, validatorClass, reportLineNumber);
    }
}
