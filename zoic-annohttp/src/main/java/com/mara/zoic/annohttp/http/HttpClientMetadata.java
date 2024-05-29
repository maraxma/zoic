package com.mara.zoic.annohttp.http;


import com.mara.zoic.annohttp.annotation.Request;
import com.mara.zoic.annohttp.http.request.converter.RequestBodyConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;
import com.mara.zoic.annohttp.http.visitor.ResponseVisitor;
import org.apache.hc.core5.http.ContentType;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * HTTP客户端元数据接口。提供和HTTP客户端很多相关的数据。
 * <p>有些方法是default方法，返回null。这些方法在后代的实现中是可选的，但是在使用时应当注意对null的判定。</p>
 * @author Mara.X.Ma
 */
public interface HttpClientMetadata {

    /**
     * 获得发起请求的方法的返回类型
     *
     * @return 返回类型
     */
    Class<?> getRequestMethodReturnClass();

    /**
     * 获得请求方法返回类型的实际类型。如果返回类是 {@link PreparingRequest} ，那么这个方法返回 {@link PreparingRequest}
     * 所包裹的 T 的实际类型。
     *
     * @return 返回的实际类型
     */
    Type getRequestMethodActualType();

    /**
     * 获得请求体装换器。
     * @return 请求体转换器
     */
    RequestBodyConverter getRequestBodyConverter();

    /**
     * 获得响应转换器。
     * @return 响应转换器
     */
    ResponseConverter getResponseConverter();

    /**
     * 获得响应访问器。
     * @return 响应访问器
     */
    ResponseVisitor getResponseVisitor();

    /**
     * 获得响应超时时间（秒）
     * @return 响应超时时间（秒）
     */
    int getResponseTimeoutInSeconds();

    /**
     * 获得请求超时时间（秒）
     * @return 请求超时时间（秒）
     */
    int getRequestTimeoutInSeconds();

    /**
     * 是否跟随重定向
     * @return 是否跟随重定向
     */
    boolean disableRedirects();

    ContentType getResponseContentType();

    Charset getResponseCharset();

    /**
     * 获得发起请求的客户端类，一般是一个标注了@Request的接口。
     *
     * @return 客户端类
     */
    default Class<?> getServiceClientClass() {
        return null;
    }

    /**
     * 获得发起请求的客户端实例，一般是一个标注了@Request的接口的实例。
     *
     * @return 客户端实例
     */
    default Object getServiceClient() {
        return null;
    }

    /**
     * 获得标注在客户端接口上的 {@link Request} 实例。
     *
     * @return {@link Request} 实例。
     */
    default Request getRequestAnnotation() {
        return null;
    }

    /**
     * 获得客户端的请求对应的方法。
     *
     * @return {@link Method} 实例。
     */
    default Method getRequestMethod() {
        return null;
    }

    /**
     * 获得请求方法上附带的参数
     *
     * @return 参数实例数组
     */
    default Object[] getRequestMethodArguments() {
        return null;
    }
}
