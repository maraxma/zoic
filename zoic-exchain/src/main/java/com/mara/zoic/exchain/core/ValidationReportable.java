package com.mara.zoic.exchain.core;


import java.util.Map;

import com.mara.zoic.exchain.core.ValidationMessage.MessageType;

/**
 * 代表在验证过程中的可汇报对象。
 * <p>可汇报对象中包含了一些用于汇报消息的方法。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-31
 */
public interface ValidationReportable {
    
    <T> void report(T forData, MessageType messageType, String message, String code, Map<String, Object> extra, Throwable cause);
    
    default void report(Object forData, MessageType messageType, String message) {
        report(forData, messageType, message, null, null, null);
    }
    
    default void report(Object forData, MessageType messageType, String message, String code) {
        report(forData, messageType, message, code, null, null);
    }
    
    default void report(Object forData, MessageType messageType, String message, String code, Map<String, Object> extra) {
        report(forData, messageType, message, code, extra, null);
    }
    
    default void reportInfo(Object forData, String message) {
        report(forData, MessageType.INFO, message, null, null, null);
    }
    
    default void reportDebug(Object forData, String message) {
        report(forData, MessageType.DEBUG, message, null, null, null);
    }
    
    default void reportWarning(Object forData, String message) {
        report(forData, MessageType.WARNING, message, null, null, null);
    }
    
    default void reportError(Object forData, String message) {
        report(forData, MessageType.ERROR, message, null, null, null);
    }
    
    default void reportFatal(Object forData, String message) {
        report(forData, MessageType.FATAL, message, null, null, null);
    }
    
    default void reportInfo(Object forData, String message, String code) {
        report(forData, MessageType.INFO, message, code, null, null);
    }
    
    default void reportDebug(Object forData, String message, String code) {
        report(forData, MessageType.DEBUG, message, code, null, null);
    }
    
    default void reportWarning(Object forData, String message, String code) {
        report(forData, MessageType.WARNING, message, code, null, null);
    }
    
    default void reportError(Object forData, String message, String code) {
        report(forData, MessageType.ERROR, message, code, null, null);
    }
    
    default void reportFatal(Object forData, String message, String code) {
        report(forData, MessageType.FATAL, message, code, null, null);
    }
}
