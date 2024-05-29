package com.mara.zoic.exchain.core;

import java.util.List;
import java.util.Map;

/**
 * 一个Validation代表包含了一个或者多个Validator的完整校验链。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-31
 */
public interface Validation {

    ValidatorNode getRootValidatorNode();

    Validation nextSerial(Validation validation);

    Validation nextParallel(Validation... validations);

    void disableValidator(String validatorName);

    void enableValidator(String validatorName);

    void removeValidator(String validatorName);

    void updateValidator(String validatorName, Class<? extends AbstractValidator<?>> validatorClass);

    void addSerialFirst(Validation validation);

    void addSerialLast(Validation validation);

    void addSerialBefore(String beforeValidatorName, Validation validation);

    void addSerialAfter(String afterValidatorName, Validation validation);

    void addParallelFirst(Validation... validations);

    void addParallelLast(Validation... validations);

    void addParallelBefore(String beforeValidatorName, Validation... validations);

    void addParallelAfter(String afterValidatorName, Validation... validations);

    Map<String, ValidatorNode> getValidators();

    String toRoadMapString();

    interface ValidatorNode {
        String getName();
        List<ValidatorNode> getParallelNodes();
        Class<? extends AbstractValidator<?>> getValidator();
        ValidatorNode getNext();
        ValidatorNode getPrev();
        ValidatorNode getContainer();
        boolean isParallelFirstNode();
        boolean isEnabled();
        default boolean isParallelNode() {
            return getValidator() == null;
        }
    }
}
