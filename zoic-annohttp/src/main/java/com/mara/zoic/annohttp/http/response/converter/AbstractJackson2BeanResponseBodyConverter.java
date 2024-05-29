package com.mara.zoic.annohttp.http.response.converter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.exception.ConversionException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.nio.charset.Charset;
import java.util.Set;

public abstract class AbstractJackson2BeanResponseBodyConverter extends AbstractAutoCloseEntityResponseBodyConverter {

    protected ObjectMapper objectMapper;
    protected Set<ContentType> acceptableContentTypes;
    protected String name;

    @Override
    public boolean canConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata,
                              ContentType computedResponseContentType, Charset computedResponseCharset) {
        return httpResponse.getEntity() != null
                && httpResponse.getEntity().isStreaming()
                && acceptableContentTypes.stream().anyMatch(e -> e.getMimeType().equalsIgnoreCase(computedResponseContentType.getMimeType()));
    }

    @Override
    public Object doConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata,
                          ContentType computedResponseContentType, Charset computedResponseCharset) {
        try {
            String jsonString = EntityUtils.toString(httpResponse.getEntity(), computedResponseCharset);
            return objectMapper.readValue(jsonString, objectMapper.constructType(metadata.getRequestMethodActualType()));
        } catch (Exception e) {
            throw new ConversionException(this, "Cannot convert response body to " + name, e);
        }
    }

}
