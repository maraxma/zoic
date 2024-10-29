package com.mara.zoic.annohttp.lifecycle;

import org.apache.hc.core5.http.ClassicHttpResponse;

import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.PreparingRequest;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;

/**
 * annohttp 生命周期接口。此接口管控整个 annohttp ，包括其生成的所有客户端。
 * <p>提供一些钩子方法，这些钩子方法一般来说是只读的。
 * <p>需要使用 {@link com.mara.zoic.annohttp.http.AnnoHttpClients#addAnnoHttpLifecycleInstances(AnnoHttpLifecycle...)} 方法注册。
 */
public interface AnnoHttpLifecycle {
    /**
     * 在客户端被创建之前触发。
     * @param clientClass 客户端类（接口）
     */
    void beforeClientCreating(Class<?> clientClass);
    /**
     * 在客户端被创建后触发。
     * @param client 已经创建出来的客户端
     */
    void afterClientCreated(Object client);
    /**
     * 在客户端发起请求之前触发。
     * @param httpClientMetadata 一些供参考的元数据
     * @param preparingRequest {@link PreparingRequest} 对象，此对象可以在请求发起之前修改一些参数
     */
    void beforeClientRequesting(HttpClientMetadata httpClientMetadata, PreparingRequest<?> preparingRequest);
    /**
     * 在客户端请求完成后触发。
     * @param httpClientMetadata 一些供参考的元数据
     * @param httpResponse 响应体
     * @param responseConverter 自动计算出的响应体转换器
     */
    void afterClientRequested(HttpClientMetadata httpClientMetadata, ClassicHttpResponse httpResponse, ResponseConverter responseConverter);
}
