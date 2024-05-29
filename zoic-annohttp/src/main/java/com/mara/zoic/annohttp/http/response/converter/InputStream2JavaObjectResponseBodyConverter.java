package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.exception.ConversionException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;

public class InputStream2JavaObjectResponseBodyConverter extends AbstractAutoCloseEntityResponseBodyConverter {

    @Override
    public boolean canConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return ContentType.APPLICATION_OCTET_STREAM.getMimeType().equalsIgnoreCase(computedResponseContentType.getMimeType()) && httpResponse.getEntity() != null && httpResponse.getEntity().isStreaming();
    }

    @Override
    public Object doConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(httpResponse.getEntity().getContent())) {
            return objectInputStream.readObject();
        } catch (UnsupportedOperationException | IOException | ClassNotFoundException e) {
            throw new ConversionException(this, "Cannot convert response", e);
        }
    }

}
