package com.mara.zoic.annohttp.http;

import org.apache.hc.core5.http.ClassicHttpResponse;

public class YamlConvertible extends AbstractJacksonConvertible {

    public YamlConvertible(ClassicHttpResponse httpResponse, String charset) {
        super(httpResponse, charset);
        objectMapper = JacksonComponentHolder.getYamlMapper(false, true, true, false);
    }
}
