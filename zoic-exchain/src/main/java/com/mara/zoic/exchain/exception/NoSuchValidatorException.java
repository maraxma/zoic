package com.mara.zoic.exchain.exception;

/**
 * 代表没有这个验证器异常。
 * @author Mara.X.Ma
 * @since 1.0.0 2021-09-25
 */
public class NoSuchValidatorException extends Exception {

    private static final long serialVersionUID = -8619391227210315623L;
    
    private final String validatorName;

    public NoSuchValidatorException(String validatorName, Throwable cause) {
        super("No such validator: " + validatorName, cause);
        this.validatorName = validatorName;
    }

    public NoSuchValidatorException(String validatorName) {
        super("No such validator: " + validatorName);
        this.validatorName = validatorName;
    }

    public String getValidatorName() {
        return validatorName;
    }
}
