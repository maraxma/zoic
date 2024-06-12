package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * Void转换器。适用于不关心响应（体）的情况。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-10-11
 */
public class VoidResponseConverter extends AbstractAutoCloseEntityResponseConverter {

    @Override
    public Object doConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return null;
    }

    @Override
    public boolean canConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        Type typeNeeded = metadata.getRequestMethodActualType();
        return typeNeeded == Void.class || typeNeeded == void.class;
    }

}
