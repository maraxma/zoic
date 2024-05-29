package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;

import java.nio.charset.Charset;

/**
 * StatusLine转换器。负责提取响应体中的StatusLine。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-10-11
 */
public class ResponseHeaderResponseConverter extends AbstractAutoCloseEntityResponseConverter {

    @Override
    public Object doConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return httpResponse.getHeaders();
    }

    @Override
    public boolean canConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return metadata.getRequestMethodActualType() instanceof @SuppressWarnings("rawtypes")Class clazz && Header[].class.isAssignableFrom(clazz);
    }

}
