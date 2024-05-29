package com.mara.zoic.exchain.core;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mara.zoic.exchain.core.ValidationMessage.MessageType;

/**
 * {@link ValidationContext} 的默认实现。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-23
 */
class DefaultValidationContext implements ValidationContext, AutoCloseable {

    /**
     * 用于传递 {@link DataProvider} 的映射
     */
    Map<String, DataProvider<?>> DATAPROVIDER_MAP = new ConcurrentHashMap<>(16);

    /**
     * 用于传递ValidationName的变量
     * <p>设定好后，才会开始正式的校验，因此这里并不需要volatile修饰
     */
    final String VALIDATION_NAME;

    /**
     * 用于传递 {@link Validation} 的对象
     */
    final Validation VALIDATION;

    /**
     * 用于传递 {@link ValidationManager} 的对象
     */
    final ValidationManager VALIDATION_MANAGER;

    /**
     * 用于传递 {@link ValidationMode} 对象
     */
    final ValidationMode VALIDATION_MODE;

    /**
     * 用于传递在即停模式中的 {@link MessageType} 对象
     */
    final MessageType STOP_MESSAGE_TYPE;

    /**
     * 用于传递ValidatorName的变量。
     * <p>由于存在并行校验，而对于ValidationContext来说只有唯一实例，因此这里要是用ThreadLocal来隔离线程之间的相影响。
     */
    InheritableThreadLocal<String> VALIDATOR_NAME = new InheritableThreadLocal<>();

    /**
     * 用于传递Validator实例的变量
     */
    InheritableThreadLocal<AbstractValidator<?>> VALIDATOR_INSTANCE = new InheritableThreadLocal<>();

    /**
     * 校验器取消功能需要使用到的取消标记。
     * 之所以是个AtomicBoolean对象，而不直接使用Boolean，是因为在其他线程内需要保存引用，使用包装型的AtomicBoolean对象
     * 才可以替代不可变对象Boolean。
     */
    ThreadLocal<AtomicBoolean> MY_CANCELLATION_FLAG = new ThreadLocal<>();

    /**
     * 存储当前节点的并行节点的取消标记，方便在运行时动态改变它们的值
     */
    ThreadLocal<List<AtomicBoolean>> PARALLEL_VALIDATORS_CANCELLATION_FLAG = new ThreadLocal<>();

    /**
     * 存储当前节点是否已经检查到取消标记。
     * <p>这个变量的存在用于辅助检测用户对 {@link CancellationException} 的捕获行为可能导致的问题，然后提醒给用户。
     */
    ThreadLocal<Boolean> CANCELLATION_CHECKED = new ThreadLocal<>();

    /**
     * 存储是否忽略接下来所有的验证器
     */
    final AtomicBoolean IGNORE_ALL_FOLLOWING_VALIDATORS = new AtomicBoolean();

    /**
     * 存储已设定的需要忽略的验证器名称
     */
    final Set<String> IGNORED_VALIDATORS = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    DefaultValidationContext(ValidationManager validationManager, Validation validation, String validationName, ValidationMode validationMode, MessageType stopMessageType) {
        VALIDATION_MANAGER = validationManager;
        VALIDATION = validation;
        VALIDATION_NAME = validationName;

        VALIDATION_MODE = validationMode;
        STOP_MESSAGE_TYPE = stopMessageType;

        MY_CANCELLATION_FLAG.set(new AtomicBoolean());
        PARALLEL_VALIDATORS_CANCELLATION_FLAG.set(Collections.emptyList());
    }

    @Override
    public DataProvider<?> get(String key) {
        return DATAPROVIDER_MAP.get(key);
    }

    @Override
    public ValidationContext put(String key, DataProvider<?> dataProvider) {
        DATAPROVIDER_MAP.put(key, dataProvider);
        return this;
    }

    @Override
    public ValidationContext remove(String key) {
        DATAPROVIDER_MAP.remove(key);
        return this;
    }

    @Override
    public ValidationContext clear() {
        DATAPROVIDER_MAP.clear();
        return this;
    }

    @Override
    public String getValidationName() {
        return VALIDATION_NAME;
    }

    @Override
    public String getValidatorName() {
        return VALIDATOR_NAME.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void report(T forData, MessageType messageType, String message, String code, Map<String, Object> extra,
            Throwable cause) {
        ((AbstractValidator<T>) VALIDATOR_INSTANCE.get()).report(forData, messageType, message, code, extra, cause);
    }

    @Override
    public AbstractValidator<?> getValidator() {
        return VALIDATOR_INSTANCE.get();
    }

    @Override
    public void checkCancellation() {
        if (MY_CANCELLATION_FLAG.get().get()) {
            CANCELLATION_CHECKED.set(true);
            throw new CancellationException();
        }
    }

    @Override
    public void cancelOtherParallelValidators() {
        List<AtomicBoolean> otherParallelValidatorsYieldFlag = PARALLEL_VALIDATORS_CANCELLATION_FLAG.get();
        if (otherParallelValidatorsYieldFlag != null) {
            otherParallelValidatorsYieldFlag.forEach(e -> e.set(true));
        }
    }

    @Override
    public boolean isCancellationChecked() {
        return CANCELLATION_CHECKED.get();
    }

    @Override
    public void ignoreAllFollowingValidators(boolean ignore) {
        IGNORE_ALL_FOLLOWING_VALIDATORS.set(ignore);
    }

    @Override
    public boolean isIgnoreAllFollowingValidators() {
        return IGNORE_ALL_FOLLOWING_VALIDATORS.get();
    }

    @Override
    public Set<String> getIgnoredValidators() {
        return Collections.unmodifiableSet(IGNORED_VALIDATORS);
    }

    @Override
    public void setIgnoreValidators(String... validatorNames) {
        synchronized (IGNORED_VALIDATORS) {
            IGNORED_VALIDATORS.clear();
            addIgnoreValidators(validatorNames);
        }
    }

    @Override
    public void addIgnoreValidators(String... validatorNames) {
        for (String validatorName : validatorNames) {
            if (validatorName == null || "".equals(validatorName.trim())) {
                throw new IllegalArgumentException("validatorName cannot be null or empty");
            }
            IGNORED_VALIDATORS.add(validatorName);
        }
    }

    @Override
    public void removeIgnoredValidators(String... validatorNames) {
        for (String validatorName : validatorNames) {
            // 不再需要判null，如果出现，ConcurrentHashMap会抛出异常
            IGNORED_VALIDATORS.remove(validatorName);
        }
    }

    @Override
    public void clearIgnoredValidators() {
        IGNORED_VALIDATORS.clear();
    }

    @Override
    public void close() {
        DATAPROVIDER_MAP.clear();
        VALIDATOR_NAME.remove();
        VALIDATOR_INSTANCE.remove();
        MY_CANCELLATION_FLAG.remove();
        PARALLEL_VALIDATORS_CANCELLATION_FLAG.remove();
        CANCELLATION_CHECKED.remove();
        DATAPROVIDER_MAP = null;
        VALIDATOR_NAME = null;
        VALIDATOR_INSTANCE = null;
        MY_CANCELLATION_FLAG = null;
        CANCELLATION_CHECKED = null;
        PARALLEL_VALIDATORS_CANCELLATION_FLAG = null;

        IGNORED_VALIDATORS.clear();
    }

    @Override
    public ValidationManager getValidationManager() {
        return VALIDATION_MANAGER;
    }

    @Override
    public Validation getValidation() {
        return VALIDATION;
    }

    @Override
    public ValidationMode getValidationMode() {
        return VALIDATION_MODE;
    }

    @Override
    public MessageType getStopMessageType() {
        return STOP_MESSAGE_TYPE;
    }
}
