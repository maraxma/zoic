package com.mara.zoic.exchain.core;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class DefaultValidationResult<T> implements ValidationResult<T> {

    T originalData;
    Set<ValidationMessage> messages = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
    String validationName;

    @Override
    public T getOriginalData() {
        return originalData;
    }

    @Override
    public Set<ValidationMessage> getMessages() {
        return Collections.unmodifiableSet(messages);
    }

    @Override
    public String getValidationName() {
        return validationName;
    }

    @Override
    public ValidationResultCondition<T> when(Function<ValidationResult<T>, Boolean> condition) {
        return new DefaultValidationResultCondition<>(condition, this);
    }

    public static void main(String[] args) {
        DefaultValidationResult<Object> result = new DefaultValidationResult<>();
        result.when(r -> r.getMessages().isEmpty()).thenThrowRuntimeException(e -> new RuntimeException("sxxd"));
    }
    
    @Override
    public String toString() {
        return originalData + ": " + messages;
    }
}
