package com.mara.zoic.annohttp.http.request.converter;

import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.exception.NoApplicableRequestBodyConverterException;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

public class AutoRequestBodyConverter implements RequestBodyConverter {

    @Override
    public HttpEntity convert(Object source, ContentType contentType, HttpClientMetadata annoHttpClientMetadata, String formFieldName) {

        for (RequestBodyConverter requestBodyConverter : RequestBodyConverterCache.getAll().values()) {
            if (requestBodyConverter.canConvert(source, contentType, annoHttpClientMetadata, formFieldName)) {
                return requestBodyConverter.convert(source, contentType, annoHttpClientMetadata, formFieldName);
            }
        }

        throw new NoApplicableRequestBodyConverterException(source, contentType, null);
    }

    @Override
    public boolean canConvert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {
        return true;
    }

}
