package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.exception.NoApplicableResponseBodyConverterException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

import java.nio.charset.Charset;

/**
 * 自动识别的响应转换器。
 * <p>因为最终调用的是具体的转换器，因此关不关闭资源去取决于具体的转换器，这里不能继承自{@link AbstractAutoCloseEntityResponseConverter}。</p>
 */
public class AutoResponseConverter implements ResponseConverter {

    @Override
    public Object convert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {

        for (ResponseConverter responseConverter : ResponseConverterCache.getAll().values()) {
            if (responseConverter.canConvert(httpResponse, metadata, computedResponseContentType, computedResponseCharset)) {
                return responseConverter.convert(httpResponse, metadata, computedResponseContentType, computedResponseCharset);
            }
        }

        // 找不到合适的转换器，那么直接抛出异常，提醒用户自己创建
        throw new NoApplicableResponseBodyConverterException(metadata.getRequestMethodActualType(), computedResponseContentType, null);
    }

    @Override
    public boolean canConvert(ClassicHttpResponse httpResponse, HttpClientMetadata metadata, ContentType computedResponseContentType, Charset computedResponseCharset) {
        return true;
    }
}
