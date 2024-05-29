package com.mara.zoic.annohttp.http.request.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Collections;

public class StringRequestBodyConverter implements RequestBodyConverter {

    @Override
    public HttpEntity convert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {
        HttpEntity httpEntity;
        if (contentType == null) {
            httpEntity = new StringEntity((String) source, DEFAULT_STRING_CHARSET);
        } else {
            Charset charset = contentType.getCharset();
            charset = charset == null ? DEFAULT_STRING_CHARSET : charset;
            String fieldName = "".equals(formFieldName) ? DEFAULT_STRING_FORM_FIELD_NAME : formFieldName;
            if (contentType.getMimeType().equalsIgnoreCase(ContentType.TEXT_PLAIN.getMimeType())) {
                httpEntity = new StringEntity((String) source, charset);
            } else if (contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
                BasicNameValuePair basicNameValuePair = new BasicNameValuePair(fieldName, (String) source);
                httpEntity = new UrlEncodedFormEntity(Collections.singletonList(basicNameValuePair), charset);
            } else if (contentType.getMimeType().equalsIgnoreCase(ContentType.MULTIPART_FORM_DATA.getMimeType())) {
                httpEntity = MultipartEntityBuilder.create()
                        .addTextBody(fieldName, (String) source)
                        .setCharset(charset)
                        .build();
            } else if (contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_OCTET_STREAM.getMimeType())) {
                httpEntity = new InputStreamEntity(new ByteArrayInputStream(((String) source).getBytes(charset)), contentType);
            } else {
                httpEntity = new StringEntity((String) source, contentType.withCharset(charset));
            }
        }

        return httpEntity;
    }

    @Override
    public boolean canConvert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {
        return source instanceof String;
    }

}
