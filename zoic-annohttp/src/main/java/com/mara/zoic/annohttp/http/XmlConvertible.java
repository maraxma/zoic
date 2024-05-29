package com.mara.zoic.annohttp.http;

import org.apache.hc.core5.http.ClassicHttpResponse;

public class XmlConvertible extends AbstractJacksonConvertible {

    public XmlConvertible(ClassicHttpResponse httpResponse, String charset) {
        super(httpResponse, charset);
        objectMapper = JacksonComponentHolder.getXmlMapper(false, true, true, false);
    }

}
