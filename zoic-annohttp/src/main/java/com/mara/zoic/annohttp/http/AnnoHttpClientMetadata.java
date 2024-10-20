package com.mara.zoic.annohttp.http;


import com.mara.zoic.annohttp.annotation.Request;
import com.mara.zoic.annohttp.http.request.converter.RequestBodyConverter;
import com.mara.zoic.annohttp.http.request.converter.RequestBodyConverterCache;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverterCache;
import com.mara.zoic.annohttp.http.visitor.ResponseVisitor;
import com.mara.zoic.annohttp.http.visitor.ResponseVisitorCache;
import org.apache.hc.core5.http.ContentType;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * 针对于AnnoHttpClient（注解驱动的HTTP客户端）的客户端元数据实现类。
 * <p>AnnoHttpClientMetadata类实现了HttpClientMetadata的所有方法并保证在运行时这些方法都不会返回null。</p>
 * @author Mara.X.Ma
 */
public class AnnoHttpClientMetadata implements HttpClientMetadata {

    Class<?> serviceClientClass;
    Object serviceClient;
    Class<?> requestMethodReturnClass;
    Request requestAnnotation;
    Method requestMethod;
    Object[] requestArguments;
    Type requestMethodReturnActualType;
    int responseTimeoutInSeconds = 180;
    int connectionRequestTimeoutInSeconds = 60;

    @Override
    public Class<?> getServiceClientClass() {
        return serviceClientClass;
    }

    @Override
    public Object getServiceClient() {
        return serviceClient;
    }

    @Override
    public Request getRequestAnnotation() {
        return requestAnnotation;
    }

    @Override
    public Method getRequestMethod() {
        return requestMethod;
    }

    @Override
    public Object[] getRequestMethodArguments() {
        return requestArguments;
    }

    @Override
    public Class<?> getRequestMethodReturnClass() {
        return requestMethodReturnClass;
    }

    @Override
    public Type getRequestMethodActualType() {
        return requestMethodReturnActualType;
    }

    @Override
    public RequestBodyConverter getRequestBodyConverter() {
        return RequestBodyConverterCache.getAll().get(getRequestAnnotation().requestBodyConverter());
    }

    @Override
    public ResponseConverter getResponseConverter() {
        return ResponseConverterCache.getAll().get(getRequestAnnotation().responseConverter());
    }

    @Override
    public ResponseVisitor getResponseVisitor() {
        return ResponseVisitorCache.getOrCreate(getRequestAnnotation().responseVisitor());
    }

    @Override
    public int getResponseTimeoutInSeconds() {
        return responseTimeoutInSeconds;
    }

    @Override
    public int getConnectionRequestTimeoutInSeconds() {
        return connectionRequestTimeoutInSeconds;
    }

    @Override
    public boolean disableRedirects() {
        return getRequestAnnotation().disableRedirects();
    }

    @Override
    public ContentType getResponseContentType() {
        return ContentType.parse(getRequestAnnotation().responseContentType());
    }

    @Override
    public Charset getResponseCharset() {
        return Charset.forName(getRequestAnnotation().responseCharset());
    }

}
