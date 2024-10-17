package com.mara.zoic.annohttp.http;

import com.mara.zoic.annohttp.annotation.Request;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Function;

public class AnnoHttpClientInvocationHandler implements InvocationHandler {

    protected String baseUrl;
    protected Function<HttpClientMetadata, String> baseUrlProvider;

    AnnoHttpClientInvocationHandler(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    AnnoHttpClientInvocationHandler(Function<HttpClientMetadata, String> baseUrlProvider) {
        this.baseUrlProvider = baseUrlProvider;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

        Class<?> returnType = method.getReturnType();
        Type genericType = method.getGenericReturnType();
        Request requestAnno = method.getAnnotation(Request.class);

        // 组装AnnotationHttpClientMetadata
        AnnoHttpClientMetadata metadata = new AnnoHttpClientMetadata();
        metadata.serviceClient = proxy;
        metadata.requestMethodReturnClass = returnType;
        metadata.serviceClientClass = method.getDeclaringClass();
        metadata.requestMethod = method;
        metadata.requestArguments = args == null ? new Object[0] : Arrays.copyOf(args, args.length);
        metadata.requestAnnotation = requestAnno;
        if (PreparingRequest.class.isAssignableFrom(returnType)) {
            // 需要构造PreparingRequest实例，延迟请求
            if (!(genericType instanceof ParameterizedType)) {
                // should never happen
                throw new IllegalStateException("PreparingRequest is not a ParameterizedType(SHOULD NEVER HAPPEN)");
            }
            metadata.requestMethodReturnActualType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        } else {
            metadata.requestMethodReturnActualType = genericType;
        }
    	
    	PreparingRequest<?> preparingRequest = new PreparingRequestImpl<>(metadata, method, args, baseUrl, baseUrlProvider);


        if (PreparingRequest.class.isAssignableFrom(returnType)) {
            // 需要构造PreparingRequest实例，延迟请求
            if (!(genericType instanceof ParameterizedType)) {
                // should never happen
                throw new IllegalStateException("PreparingRequest is not a ParameterizedType(SHOULD NEVER HAPPEN)");
            }
            metadata.requestMethodReturnActualType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            return preparingRequest;
        } else {
            metadata.requestMethodReturnActualType = genericType;
            return preparingRequest.request();
        }
    }
}
