package com.mara.zoic.annohttp.http.request.converter;

import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.Converter;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.Charset;

public class ByteArrayRequestBodyConverter implements RequestBodyConverter {

    @Override
    public HttpEntity convert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {
        HttpEntity httpEntity;
        if (contentType == null) {
            httpEntity = new ByteArrayEntity((byte[]) source, ContentType.APPLICATION_OCTET_STREAM);
        } else {
            if (ContentType.TEXT_PLAIN.getMimeType().equalsIgnoreCase(contentType.getMimeType())) {
                Charset charset = contentType.getCharset();
                charset = charset == null ? Converter.DEFAULT_STRING_CHARSET : charset;
                httpEntity = new StringEntity(new String((byte[]) source, charset), charset);
            } else {
                throw new IllegalArgumentException("If @Body represent a byte[], its content type can application/octet-stream or text/plain only, or you can set no content type, annohttp set it as application/octet-stream by default");
            }
        }

        return httpEntity;
    }

    @Override
    public boolean canConvert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {
        return source instanceof byte[];
    }

}
