package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.exception.ConversionException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.nio.charset.Charset;

public class StringResponseBodyConverter extends AbstractAutoCloseEntityResponseBodyConverter {

    @Override
    public Object doConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        try {
            // 这个方法会自动关闭流
            byte[] bytes = EntityUtils.toByteArray(httpResponse.getEntity());
            if (bytes == null) {
                return null;
            }
            return new String(bytes, computedResponseCharset);
        } catch (Exception e) {
            throw new ConversionException(this, "Cannot convert response body to String with charset '" + computedResponseCharset + "'", e);
        }
    }

    @Override
    public boolean canConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadataContentType, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return metadataContentType.getRequestMethodActualType() == String.class
                && httpResponse.getEntity() != null && httpResponse.getEntity().isStreaming();
    }
}
