package com.mara.zoic.exchain.core;


import com.mara.zoic.exchain.core.ValidationMessage.MessageType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 抽象验证器。
 * <p>实现类并重写 {@link #validate(Set, ValidationContext)} 方法以实现自己的验证逻辑。
 * <p>重写后，使用 {@link DefaultValidation} 来编排验证器，然后使用 {@link DefaultValidationManager} 来开始校验你的数据。
 * @author Mara.X.Ma
 * @since 1.0.0 2021-03-31
 * @see DefaultValidation
 * @see DefaultValidationManager
 * @see ValidationContext
 * @see ValidationRecord
 * @see ValidationResult
 * @see ValidationMessage
 * @see #report(Object, MessageType, String, String, Map, Throwable)
 * @see ValidationContext#report(Object, MessageType, String, String, Map, Throwable)
 * @see DataWrapper#report(MessageType, String)
 */
public abstract class AbstractValidator<T> {

    private static final String METHOD_NAME_INNERVALIDATE = "org.mosin.bocus.core.vd.DefaultValidationManager.validate";

    /**
     * 存储在验证过程中产生的验证结果。
     * <p>使用ThreadLocal的目的在于汇报需要线程隔离。
     * <p>使用InheritableThreadLocal的目的在于可以使用户在自己的子线程中汇报消息。
     */
    InheritableThreadLocal<ValidationRecord<T>> result = new InheritableThreadLocal<>();
    InheritableThreadLocal<Map<T, DataWrapper<T>>> cachedDataWrappers = new InheritableThreadLocal<>();

    @SuppressWarnings("unchecked")
    final ValidationRecord<T> innerValidate(Set<DataWrapper<T>> data, ValidationContext validationContext) {
        try {
            cachedDataWrappers.set(data.stream().collect(Collectors.toMap(DataWrapper::getOriginalData, Function.identity())));
            DefaultValidationRecord<T> validationRecord = new DefaultValidationRecord<>();
            result.set(validationRecord);
            validationRecord.startDateTime = ZonedDateTime.now();
            validationRecord.validationName = validationContext.getValidationName();
            validationRecord.validatorName = validationContext.getValidatorName();
            validationRecord.validatorClass = (Class<? extends AbstractValidator<?>>) getClass();
            boolean cancellationExceptionCaught = false;
            try {
                validate(data, validationContext);
            } catch (CancellationException e) {
                // IGNORE
                // 可以采用日志方式记录，以方便调试
                cancellationExceptionCaught = true;
            }
            boolean cancellationChecked = validationContext.isCancellationChecked();
            if (cancellationChecked && !cancellationExceptionCaught) {
                // 当用户检查了取消点但是程序没有捕获到CancellationException时抛出此错误
                // 此错误旨在提醒用户不要手动捕获CancellationException异常，否则会导致取消功能失效
                throw new IllegalStateException("Detected cancellation, but there's no CancellationException caught, please DO NOT catch CancellationException for `checkCancellation` method by " +
                        "yourself, or else the cancellation for parallel validators will be fail");
            }
            validationRecord.endDateTime = ZonedDateTime.now();
            validationRecord.duration = Duration.between(validationRecord.endDateTime, validationRecord.startDateTime);
            validationRecord.originalData = data.stream().map(DataWrapper::getOriginalData).collect(Collectors.toSet());
            return result.get();
        } finally {
            result.remove();
            cachedDataWrappers.remove();
        }
    }

    /**
     * 核心方法，重写此方法以实现用户自己的验证逻辑。
     * @param data 数据
     * @param validationContext 验证上下文，提供一些既有的信息，也可以通过它在各个Validator中传递用户自己的信息
     */
    protected void validate(Set<DataWrapper<T>> data, ValidationContext validationContext) {
        // NOP for default, can define empty validator quickly
    }

    /**
     * 为指定的数据汇报校验结果。
     * @param forData 数据，不能是null
     * @param messageType 消息类型，不能是null
     * @param message 信息，不能是null
     * @param code 代码，可以是null
     * @param extra 额外信息，可以是null
     * @param cause 异常，可以是null
     */
    protected final void report(T forData, MessageType messageType, String message, String code, Map<String, Object> extra, Throwable cause) {
        Objects.requireNonNull(forData, "`forData` cannot be null");
        Objects.requireNonNull(messageType, "`messageType cannot be null`");
        requireNonNullOrEmpty(message, "`message` cannot be null or empty");
        DefaultValidationRecord<T> validationRecord = (DefaultValidationRecord<T>) result.get();
        Map<T, ValidationResult<T>> validationResults = validationRecord.validationResults;
        validationResults.compute(forData, (k, v) -> {
            DefaultValidationResult<T> dvr;
            if (v == null) {
                dvr = new DefaultValidationResult<>();
                dvr.originalData = forData;
                dvr.validationName = validationRecord.validationName;
            } else {
                dvr = (DefaultValidationResult<T>) v;
            }
            DefaultValidationMessage msg = new DefaultValidationMessage(message);
            msg.reportTime = ZonedDateTime.now();
            msg.messageType = messageType;
            msg.code = code;
            msg.cause = cause;
            if (extra != null) {
                msg.extra = new HashMap<>(extra);
            }
            msg.validationName = validationRecord.validationName;
            msg.validatorName = validationRecord.validatorName;
            msg.validatorClass = validationRecord.validatorClass;
            fillInnerFields(msg);
            dvr.messages.add(msg);

            // 为该数据添加已有的校验信息
            DataWrapper<T> dw = cachedDataWrappers.get().get(forData);
            DefaultValidationResult<T> validatedInfo = (DefaultValidationResult<T>) dw.getValidatedInfo();
            validatedInfo.messages.add(msg);

            // 判定校验模式
            ValidationContext validationContext = ValidationContexts.autoDetect();
            ValidationMode validationMode = validationContext.getValidationMode();
            MessageType expectedType = validationContext.getStopMessageType();
            if (expectedType == null || messageType.isSeriousThan(expectedType) || messageType == expectedType) {
                if (validationMode == ValidationMode.IMMEDIATE_STOP_SINGLE_DATA) {
                    dw.setIgnoreAllFollowingValidators(true);
                } else if (validationMode == ValidationMode.IMMEDIATE_STOP_ALL_DATA) {
                    validationContext.ignoreAllFollowingValidators(true);
                }
            }

            return dvr;
        });
    }

    private void fillInnerFields(DefaultValidationMessage msg) {
        Type genericType = null;
        try {
            genericType = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        } catch (Exception e) {
            // IGNORE
        }
        Class<?> validatorAcceptableClass = (Class<?>) genericType;

        StackTraceElement reportCallerStackTraceElement = findRealReportCaller();
        StackTraceElement validatorUserCallerStackTraceElement = findCaller(METHOD_NAME_INNERVALIDATE);

        msg.reportCallerTrace = reportCallerStackTraceElement;
        msg.validatorCallerTrace = validatorUserCallerStackTraceElement;
        msg.validatorAcceptableClass = validatorAcceptableClass;
        msg.reportLineNumber = reportCallerStackTraceElement == null ? - 1 : reportCallerStackTraceElement.getLineNumber();
        msg.reportThreadName = Thread.currentThread().getName();
    }

    /**
     * 调用此方法以在并发校验中确认检查点。
     * <p>如果其他线程要求此线程yield，那么此线程将会在此中断。
     * <p>这适用于并行校验中的互斥校验。
     * <p>必须要有此方法的调用才能设定有效的退出点。然而，并行校验并不能精确控制退出时间点，这取决于程序的运行状况。
     * <p>请不要手动捕获此方法抛出的{@link CancellationException}，否则将导致并行验证器取消功能失效。
     */
    protected final void checkCancellation() {
        // 检查自己的状态，如果需要被让出，那么抛出异常让Manager捕获
        // 由于Java的线程取消机制，这里只能采取这样的方式
        ValidationContexts.autoDetect().checkCancellation();
    }

    /**
     * 取消其他处于同一并行级别的校验器的校验流程。
     */
    protected final void cancelOtherParallelValidators() {
        ValidationContexts.autoDetect().cancelOtherParallelValidators();
    }

    private static StackTraceElement findRealReportCaller() {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stes) {
            Class<?> clazz;
            try {
                clazz = Class.forName(ste.getClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot get class in StackTrace", e);
            }
            if (AbstractValidator.class.isAssignableFrom(clazz) && !clazz.getName().equals(AbstractValidator.class.getName())) {
                return ste;
            }
        }
        return null;
    }

    /**
     * 找到含有callerFlag的堆栈行的调用行。
     * @param callerFlag 标识
     * @return 堆栈数组，长度是2，分别代表[目标帧，调用者帧]
     */
    private static StackTraceElement findCaller(String callerFlag) {
        StackTraceElement ste = null;
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        int len = stes.length;
        for (int i = 0; i < len; i++) {
            if (stes[i].toString().contains(callerFlag)) {
                int nextIndex = i + 1;
                if (nextIndex < len && !stes[nextIndex].toString().contains(callerFlag)) {
                    ste = stes[nextIndex];
                    break;
                }
            }
        }
        return ste;
    }

    private void requireNonNullOrEmpty(Object o, String message) {
        if (o == null || (o instanceof String && "".equals(((String) o).trim()))) {
            throw new IllegalArgumentException(message);
        }
    }
}
