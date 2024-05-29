package com.mara.zoic.annohttp.http.exception;

import com.mara.zoic.annohttp.http.Converter;

import java.io.Serial;

public class ConversionException extends RuntimeException {

    protected Converter converter;

    @Serial
    private static final long serialVersionUID = -3866209046662394762L;

    public ConversionException(Converter converter, String message, Throwable cause) {
        this((converter != null ? "[" + converter.getClass().getName() + "] " : "") + message, cause);
        this.converter = converter;
    }

    public ConversionException() {
        super();
    }

    public ConversionException(String message, Throwable cause, boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(Throwable cause) {
        super(cause);
    }

    public Converter getConverter() {
        return converter;
    }

}
