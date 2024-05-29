package com.mara.zoic.exchain.core;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class DefaultValidationRecord<T> implements ValidationRecord<T> {
    
    Set<T> originalData;
    
    ZonedDateTime startDateTime;
    ZonedDateTime endDateTime;
    String validationName;
    String validatorName;
    Class<? extends AbstractValidator<?>> validatorClass;
    Duration duration;
    
    Map<T, ValidationResult<T>> validationResults = new ConcurrentHashMap<>(16);

    @Override
    public ZonedDateTime getStartDateTime() {
        return startDateTime;
    }

    @Override
    public ZonedDateTime getEndDateTime() {
        return endDateTime;
    }

    @Override
    public String getValidationName() {
        return validationName;
    }

    @Override
    public String getValidatorName() {
        return validationName;
    }

    @Override
    public Class<? extends AbstractValidator<?>> getValidatorClass() {
        return validatorClass;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public Set<T> getOriginalData() {
        return originalData;
    }

    @Override
    public Map<T, ValidationResult<T>> getValidationResults() {
        return Collections.unmodifiableMap(validationResults);
    }
    
}
