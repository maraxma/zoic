package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.exception.ConversionException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

/**
 * 获得原生流的响应体转换器。
 * <p>因为要获得原生流，所以这里不能自动关闭，因此不能继承自 {@link AbstractAutoCloseEntityResponseBodyConverter}。</p>
 */
public class InputStreamResponseBodyConverter implements ResponseBodyConverter {

    @Override
    public Object convert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return Optional.ofNullable(httpResponse.getEntity()).map(entity -> {
            try {
                return entity.getContent();
            } catch (IOException e) {
                throw new ConversionException(this, "Cannot convert response body to InputStream", e);
            }
        }).orElse(null);
    }

    @Override
    public boolean canConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return metadata.getRequestMethodActualType() instanceof @SuppressWarnings("rawtypes")Class clazz && InputStream.class.isAssignableFrom(clazz)
                && httpResponse.getEntity() != null && httpResponse.getEntity().isStreaming();
    }

}
