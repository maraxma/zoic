package com.mara.zoic.exchain.core;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * 代表一个单一的验证结果信息。
 * <p>验证结果信息是附加到数据上的，代表了对该数据的校验结果中的一个自定义信息。
 * <p>一般来说，验证结果信息是由 {@link AbstractValidator#report(Object, MessageType, String, String, Map, Throwable)} 方法报告的。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-26
 */
public interface ValidationMessage {
    
    /**
     * 获得此消息的类型。
     * <p>应该返回一个非空的结果。
     */
    MessageType getMessageType();
    
    /**
     * 获得具体的消息。
     * <p>应该返回一个非空的结果。
     * @return 消息
     */
    String getMessage();

    /**
     * 获得与此消息相对应的代码。可以返回null。
     * @return 代码
     */
    String getCode();

    /**
     * 获得相关的字段。可以是null。
     * @return 字段名称
     */
    String getField();

    /**
     * 获得导致此信息的异常信息。可以是null。
     * @return 异常信息
     */
    Throwable getCause();

    /**
     * 获得额外的附加信息。可以是null。
     * <p>经由 {@link ReportExtender} 扩展的字段也位于这当中。
     * @return 附加信息
     */
    Map<String, Object> getExtra();

    /**
     * 获得产生此消息的验证名称。
     * <p>应该返回一个非空的结果。
     * @return 验证名称
     */
    String getValidationName();

    /**
     * 获得产生此消息的验证器名称。
     * <p>应该返回一个非空的结果。
     * @return 验证器名称
     */
    String getValidatorName();

    /**
     * 获得产生此消息的验证器的具体类。
     * <p>应该返回一个非空的结果。
     * @return 验证器类
     */
    Class<? extends AbstractValidator<?>> getValidatorClass();

    /**
     * 获得汇报此消息的代码的具体行号。
     * <p>一定存在，且为非负数。
     * @return 行号
     */
    int getReportLineNumber();

    /**
     * 获得验证器可接受的类（即定义在AbstractValidator上的具体被验证类型），由程序自行附加的，可能是null
     */
    Class<?> getValidatorAcceptableClass();
    
    /**
     * 获得汇报此消息的堆栈。
     * @return 堆栈
     */
    StackTraceElement getReportCallerTrace();

    /**
     * 获得使用这个验证器的堆栈。
     * @return 堆栈
     */
    StackTraceElement getValidatorCallerTrace();
    
    /**
     * 获得数据在该验证器上的汇报此消息的时间点。
     * <p>应该返回一个非空的结果。
     * @return 汇报消息时间点
     */
    ZonedDateTime getReportDateTime();
    
    /**
     * 获得产生此报告的线程名称
     * @return 线程名称
     */
    String getReportThreadName();
    
    /**
     * 消息类型枚举。
     * @author Mara.X.Ma
     * @Since 1.0.0 2022-03-29
     */
    enum MessageType {
        /**
         * 调试信息
         */
        DEBUG,
        
        /**
         * 一般信息
         */
        INFO,
        
        /**
         * 警告信息
         */
        WARNING,
        
        /**
         * 错误信息
         */
        ERROR,
        
        /**
         * 严重错误信息
         */
        FATAL;

        /**
         * 判定此消息类型是否比指定的消息类型严重。
         * @return true代表是，false代表否
         */
        public boolean isSeriousThan(MessageType messageType) {
            return compareTo(messageType) > 0;
        }
    }
}
