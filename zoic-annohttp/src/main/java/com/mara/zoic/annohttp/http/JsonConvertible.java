package com.mara.zoic.annohttp.http;

import org.apache.hc.core5.http.ClassicHttpResponse;

public class JsonConvertible extends AbstractJacksonConvertible {

    protected JsonConvertible(ClassicHttpResponse httpResponse, String charset) {
        super(httpResponse, charset);
        this.objectMapper = JacksonComponentHolder.getJsonMapper(false, true, true, false);
    }
}
