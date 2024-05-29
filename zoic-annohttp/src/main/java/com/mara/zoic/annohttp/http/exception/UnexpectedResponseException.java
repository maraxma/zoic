package com.mara.zoic.annohttp.http.exception;


import org.apache.hc.core5.http.HttpResponse;

import java.io.Serial;

public class UnexpectedResponseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -92981615956413451L;
    private HttpResponse httpResponse;

    public UnexpectedResponseException() {
        super();
    }

    public UnexpectedResponseException(String message, Throwable cause, boolean enableSuppression,
                                       boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UnexpectedResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedResponseException(String message) {
        super(message);
    }

    public UnexpectedResponseException(Throwable cause) {
        super(cause);
    }

    public UnexpectedResponseException withHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
        return this;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public Integer getStatusCode() {
        if (httpResponse == null) {
            return null;
        }
        return httpResponse.getCode();
    }
}
