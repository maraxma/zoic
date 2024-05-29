package com.mara.zoic.exchain.exception;

/**
 * 代表数据提供器产生的异常。
 * @author Mara.X.Ma
 * @since 1.0.0 2021-09-25
 */
public class DataProviderException extends RuntimeException {

    private static final long serialVersionUID = 3442812428174168048L;

    public DataProviderException() {
        super();
    }

    public DataProviderException(String message) {
        super(message);
    }

    public DataProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataProviderException(Throwable cause) {
        super(cause);
    }

    protected DataProviderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
