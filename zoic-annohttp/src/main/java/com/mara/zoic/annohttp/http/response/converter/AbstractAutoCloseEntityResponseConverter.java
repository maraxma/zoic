package com.mara.zoic.annohttp.http.response.converter;

import com.mara.zoic.annohttp.http.HttpClientMetadata;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public abstract class AbstractAutoCloseEntityResponseConverter implements ResponseConverter {
    @Override
    public Object convert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        try {
            return doConvert(httpResponse, metadata, computedResponseContentType, computedResponseCharset);
        } finally {
            try {
                httpResponse.close();
            } catch (IOException e) {
                // ignore
            }
            EntityUtils.consumeQuietly(httpResponse.getEntity());
        }
    }

    protected abstract  Object doConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset);
}
