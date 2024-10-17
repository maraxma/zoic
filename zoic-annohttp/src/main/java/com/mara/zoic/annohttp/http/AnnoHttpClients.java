package com.mara.zoic.annohttp.http;


import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.mara.zoic.annohttp.annotation.Request;
import com.mara.zoic.annohttp.http.protocol.ProtocolHandler;
import com.mara.zoic.annohttp.http.protocol.ProtocolHandlerMapping;
import com.mara.zoic.annohttp.http.request.converter.AutoRequestBodyConverter;
import com.mara.zoic.annohttp.http.request.converter.RequestBodyConverter;
import com.mara.zoic.annohttp.http.request.converter.RequestBodyConverterCache;
import com.mara.zoic.annohttp.http.response.converter.AutoResponseConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverterCache;
import com.mara.zoic.annohttp.lifecycle.AnnoHttpLifecycle;

/**
 * AnnoHttp的主要入口API，负责帮助用户创建请求实例。
 *
 * <p>创建一个注解驱动的请求实例需要如下两步：</p>
 * <ol>
 *    <li>创建接口，定义请求方法，并在请求方法上使用annohttp提供的注解申明请求行为；</li>
 *    <li>使用 {@link AnnoHttpClients#create(Class)} 方法传入接口类，创建真正的实例。</li>
 * </ol>
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 */
public final class AnnoHttpClients {

    /**
     * 为请求接口创建具体的实例。创建好后，你将可以直接调用。
     *
     * @param <T>           实例接口类型
     * @param annoHttpClass 请求接口类
     * @return 接口实例
     */
    public static <T> T create(Class<T> annoHttpClass) {
        // 动态代理
        // 都是基于接口的，因此使用JDK自带的动态代理即可
        return create(annoHttpClass, "");
    }

    /**
     * 为请求接口创建具体的实例。创建好后，你将可以直接调用。
     *
     * @param <T>           实例接口类型
     * @param annoHttpClass 请求接口类
     * @param baseUrl       基础URL，可以是null，null代表不附加baseUrl
     * @return 接口实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> annoHttpClass, String baseUrl) {
    	executeLifecycleBeforeCreatingMethod(annoHttpClass);
        // 动态代理
        // 都是基于接口的，因此使用JDK自带的动态代理即可
        T client = (T) Proxy.newProxyInstance(annoHttpClass.getClassLoader(), new Class<?>[]{annoHttpClass}, InvocationHandlerHolder.getOrCreateAnnoHttpClientInvocationHandler(baseUrl));
        executeLifecycleAfterCreatedMethod(client);
        return client;
    }

    /**
     * 为请求接口创建具体的实例。创建好后，你将可以直接调用。
     *
     * @param <T>           实例接口类型
     * @param annoHttpClass 请求接口类
     * @param baseUrlProvider 基础URL提供器，可以是null，null代表不附加baseUrl
     * @return 接口实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> annoHttpClass, Function<HttpClientMetadata, String> baseUrlProvider) {
    	executeLifecycleBeforeCreatingMethod(annoHttpClass);
        // 动态代理
        // 都是基于接口的，因此使用JDK自带的动态代理即可
        T client = (T) Proxy.newProxyInstance(annoHttpClass.getClassLoader(), new Class<?>[]{annoHttpClass}, new AnnoHttpClientInvocationHandler(baseUrlProvider));
        executeLifecycleAfterCreatedMethod(client);
        return client;
    }

    /**
     * 注册请求体转换器。你可以定义自己的转换器，然后使用默认的 {@link AutoRequestBodyConverter} 来查找并应用。
     * <p>你也可以直接将自己的转换器写到 {@link Request#requestBodyConverter()} 上。
     *
     * @param requestBodyConverters 请求体转换器
     */
    public static void registerRequestBodyConverter(RequestBodyConverter... requestBodyConverters) {
        RequestBodyConverterCache.addUserConverters(requestBodyConverters);
    }

    /**
     * 注册响应转换器。你可以定义自己的转换器，然后使用默认的 {@link AutoResponseConverter} 来查找并应用。
     * <p>你也可以直接将自己的转换器写到 {@link Request#responseConverter()} 上。
     *
     * @param responseConverters 响应转换器
     */
    public static void registerResponseConverter(ResponseConverter... responseConverters) {
        ResponseConverterCache.addUserConverters(responseConverters);
    }

    /**
     * 注册自定义的协议处理器。
     * @param protocolHandlers 协议处理器
     */
    public static void registerProtocolHandler(ProtocolHandler... protocolHandlers) {
        ProtocolHandlerMapping.addMappings(protocolHandlers);
    }

    /**
	 * 添加 annohttp 生命周期实例。
	 * @param annoHttpLifecycles 生命周期实例。存在多个实例时按照添加的顺序执行
	 */
	public static void addAnnoHttpLifecycleInstances(AnnoHttpLifecycle... annoHttpLifecycles) {
		AnnoHttpLifecycleInstancesCahce.addAnnoHttpLifecycleInstances(annoHttpLifecycles);
	}
	
	private static void executeLifecycleBeforeCreatingMethod(Class<?> clazz) {
		for (AnnoHttpLifecycle lifecycle : AnnoHttpLifecycleInstancesCahce.getAnnoHttpLifecycleInstances()) {
    		lifecycle.beforeClientCreating(clazz);
    	}
	}
	
	private static void executeLifecycleAfterCreatedMethod(Object client) {
		for (AnnoHttpLifecycle lifecycle : AnnoHttpLifecycleInstancesCahce.getAnnoHttpLifecycleInstances()) {
    		lifecycle.afterClientCreated(client);
    	}
	}

	private static class InvocationHandlerHolder {
        private static final AnnoHttpClientInvocationHandler INSTANCE_WITHOUT_BASE_URL = new AnnoHttpClientInvocationHandler("");

        private static final Map<String, AnnoHttpClientInvocationHandler> INSTANCES = new ConcurrentHashMap<>();

        static AnnoHttpClientInvocationHandler getOrCreateAnnoHttpClientInvocationHandler(String baseUrl) {
            if (null == baseUrl || "".equals(baseUrl.trim())) {
                return INSTANCE_WITHOUT_BASE_URL;
            } else {
                return INSTANCES.compute(baseUrl.toLowerCase().trim(), (s, annoHttpClientInvocationHandler) -> Objects.requireNonNullElseGet(annoHttpClientInvocationHandler, () -> new AnnoHttpClientInvocationHandler(baseUrl)));
            }
        }
    }
}
