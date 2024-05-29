package com.mara.zoic.exchain.core;

import java.util.function.Consumer;
import java.util.function.Function;

class DefaultValidationResultCondition<T> implements ValidationResultCondition<T> {

    final boolean matched;
    final ValidationResult<T> validationResult;

    public DefaultValidationResultCondition(Function<ValidationResult<T>, Boolean> condition, ValidationResult<T> validationResult) {
        this.validationResult = validationResult;
        matched = condition.apply(validationResult);
    }

    @Override
    public void then(Consumer<ValidationResult<T>> action) {
        if (matched) {
            action.accept(validationResult);
        }
    }

    @Override
    public <R> R thenReturn(Function<ValidationResult<T>, R> operation) {
        return thenReturn(operation, null);
    }

    @Override
    public <R> R thenReturn(Function<ValidationResult<T>, R> operation, R notMatchReturn) {
        if (matched) {
            return operation.apply(validationResult);
        }
        return notMatchReturn;
    }

    @Override
    public void thenThrowRuntimeException(Function<ValidationResult<T>, ? extends RuntimeException> exceptionGenerator) {
        if (matched) {
            RuntimeException e = exceptionGenerator.apply(validationResult);
            if (e != null) {
                throw e;
            }
        }
    }
}
