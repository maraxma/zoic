package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.exception.ConversionException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class ByteArrayResponseBodyConverter extends AbstractAutoCloseEntityResponseBodyConverter {

    @Override
    public Object doConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        try {
            return EntityUtils.toByteArray(httpResponse.getEntity());
        } catch (IOException e) {
            throw new ConversionException(this, "Cannot convert response body to byte array", e);
        }
    }

    @Override
    public boolean canConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return metadata.getRequestMethodActualType() instanceof @SuppressWarnings("rawtypes")Class clazz
                && byte[].class.isAssignableFrom(clazz)
                && httpResponse.getEntity() != null && httpResponse.getEntity().isStreaming();
    }
}
