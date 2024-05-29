package com.mara.zoic.exchain.exception;

/**
 * 异常：不是校验环境。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-31
 */
public class NotValidationEnvironmentException extends RuntimeException {

    private static final long serialVersionUID = 548329851449958355L;

    public NotValidationEnvironmentException() {
        super();
    }

    public NotValidationEnvironmentException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NotValidationEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotValidationEnvironmentException(String message) {
        super(message);
    }

    public NotValidationEnvironmentException(Throwable cause) {
        super(cause);
    }
}
