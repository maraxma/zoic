package com.mara.zoic.annohttp.http;


import com.mara.zoic.annohttp.annotation.Request;
import com.mara.zoic.annohttp.http.exception.ConversionException;
import com.mara.zoic.annohttp.http.exception.RequestFailedException;
import com.mara.zoic.annohttp.http.exception.UnexpectedResponseException;
import com.mara.zoic.annohttp.http.proxy.RequestProxy;
import com.mara.zoic.annohttp.http.request.converter.RequestBodyConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseBodyConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 代表一个已经准备好的请求。
 * <p>在接口中任何标注了 {@link Request} 的抽象方法除了可以使用期望的实体类来作为返回，还可以使用此接口类作为返回。
 * <p>使用此接口作为返回意味着该方法在被调用的时候请求不会立即进行HTTP请求，而是生成一个 {@link PreparingRequest} 对象供用户使用。
 * <p>{@link PreparingRequest} 对象提供了一些在请求前做最后准备的方法，供用户再次设定请求头、请求参数等，同时提供同步请求、异步请求、可操作对象请求（{@link #requestOperable()}）等方法。
 * <p>如果你的请求方法期望的是通过异步的手段请求，亦或是在请求前需要设定动态计算的参数，又或者是你想要得到方便转换的的响应对象，那么你可以将此作为请求方法的返回类型。
 * <p>{@link PreparingRequest} 的泛型参数T需要指定你期望的返回结果，它可以是 {@link String}、{@link Map}、{@link ClassicHttpResponse}、{@link InputStream}、Java Bean
 * 甚至是任何类型，annohttp将使用内置的转换器（{@link ResponseBodyConverter}）尝试将其转换。当然，你也可以自定义你的转换器。如果需要定义你的转换器，
 * 实现 {@link ResponseConverter} 并将其使用 {@link AnnoHttpClients#registerResponseConverter(ResponseConverter...)} 注册。注册后当符合条件时将优先使用
 * 用户自定义的转换器。
 * <p>此类中的大部分方法（{@link #request()}、{@link #requestOperable()}）都是针对<b>响应体（Response Body）的，因此如果需要处理响应体之外的内容（如状态行、响应头）等请使用
 * {@link #requestClassically()}得到 {@link ClassicHttpResponse} 对象然后自行获得。或者直接设定 Header[] 、{@link org.apache.hc.core5.http.message.StatusLine} 作为返回值。</b></p>
 * <p>不推荐将StatusLine、Header[]等特殊类型放入 PreparingRequest 作为 T，因为 PreparingRequest 中的 <code>T</code> 是专门处理（指代）响应体的。</p>
 * @param <T> 期望返回的<b>响应体</b>的数据类型。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 * @see AnnoHttpClients
 * @see RequestBodyConverter
 * @see ResponseBodyConverter
 */
public sealed interface PreparingRequest<T> permits PreparingRequestImpl {

    String MAP_KEY_NAME = "name";
    String MAP_KEY_VALUE = "value";
    String MAP_KEY_COVERABLE = "coverable";

    String DEFAULT_BYTES_FIELD_NAME = "Bytes";
    String DEFAULT_STRING_FIELD_NAME = "String";
    String DEFAULT_OBJECT_FIELD_NAME = "Object";

    /**
     * 自定义HttpClient相关的配置。需要用户自己提供一个HttpClientBuilder实例。在配置完成后将以用户提供的建造者生产新HttpClient发起请求。</p>
     * <p>为了性能，annohttp采用单例的HttpClient发起所有相关的请求，但此方法会打破规则使用用户定义好的HttpClientBuilder生成新的HttpClient发起请求。</p>
     * <p style="color: red">特别注意：一般情况下不推荐使用此方法，除非有annohttp不能解决的特殊需求。<b>使用此方法后会导致内置的代理支持出现问题，用户需要自行处理，如果确实需要保持代理的正确性，请参见 {@link HttpClientBuilderEnhancer#enhance(HttpClientBuilder)} 方法的源码，或者直接调用此方法（与你的目的不冲突的话）。</b></p>
     * <p>和 Spring 进行相关集成的时候，因为某些配置是应用于内置的 HttpClientBuilder 上，因此使用此方法自定义 HttpClient 可能会导致书写在 Spring 配置文件中的某些配置失效。</p>
     *
     * @param httpClientBuilderSupplier HttpClient建造者提供器。
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customHttpClient(Supplier<HttpClientBuilder> httpClientBuilderSupplier);

    /**
     * 自定义请求头。
     *
     * @param headerConsumer 已经有的请求头列表，是可读可写的
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customRequestHeaders(Consumer<List<CoverableNameValuePair>> headerConsumer);

    /**
     * 自定义请求参数。
     *
     * @param queryConsumer 已经有的请求参数列表，是可读可写的
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customRequestQueries(Consumer<List<CoverableNameValuePair>> queryConsumer);

    /**
     * 自定义路径参数。
     *
     * @param pathVarConsumer 已经有的路径参数列表，是可读可写的
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customRequestPathVars(Consumer<Map<String, String>> pathVarConsumer);

    /**
     * 自定义表单参数。
     *
     * @param formFieldConsumer 已经有的表单参数列表，是可读可写的
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customRequestFormFields(Consumer<Map<String, Object>> formFieldConsumer);

    /**
     * 自定义请求地址。
     *
     * @param uriMapping URI映射函数，提供原有的URI，返回你需要定制的URI
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customRequestUri(Function<String, String> uriMapping);

    /**
     * 自定义请求代理。
     * @param proxyMapping 代理映射器函数，函数提供原有的代理（如果有的话，否则是null），函数需要你返回你定义的代理，可以是null。是null时代表不设定代理
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customRequestProxy(Function<RequestProxy, RequestProxy> proxyMapping);

    /**
     * 自定义请求方法。
     * @param httpMethodMapping 请求方法映射函数，函数提供原有的请求方法，要求返回新的额请求方法，不能返回null
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customHttpMethod(Function<HttpMethod, HttpMethod> httpMethodMapping);

    /**
     * 自定义请求体。
     * @param requestBodyMapping 请求体映射函数。函数提供原有的请求体，要求返回新的请求体，可以返回null，返回null代表不再附加请求体
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customRequestBody(Function<HttpEntity, HttpEntity> requestBodyMapping);

    /**
     * 自定义请求设置
     *
     * @param requestConfigBuilderConsumer 请求设置对象建造者消费器
     * @return {@link PreparingRequest} 本身
     */
    PreparingRequest<T> customRequestConfig(Consumer<RequestConfig.Builder> requestConfigBuilderConsumer);

    /**
     * 同步请求并返回响应。直接将响应转换为用户定义在返回值中的形式。
     * <p>此方法可能会抛出三种类型的异常，但它们都是运行时异常，用户可以选择性处理。</p>
     *
     * @return 响应体
     * @throws ConversionException         请求成功，但是在转换响应的时候出现异常（可能是用户的转换器中有异常，亦或是无法将响应转换为目标对象）
     * @throws RequestFailedException      请求失败时抛出，请求失败意味着服务端异常或者网络异常，当出现此异常时甚至连 {@link ClassicHttpResponse} 都未生成
     * @throws UnexpectedResponseException 当出现意料之外的响应时抛出此异常（一般来说是@Request.successCondition校验未通过）
     */
    T request() throws RequestFailedException, UnexpectedResponseException, ConversionException;

    /**
     * 异步请求并返回响应。直接将响应转换为用户定义在返回值中的形式。这是 {@link #request()} 方法的异步版本。
     *
     * @param executorService 线程池
     * @return 未来对象
     * @see #request()
     */
    CompletableFuture<T> requestAsync(Executor executorService);

    /**
     * 异步请求并返回响应（带回调）。直接将响应转换为用户定义在返回值中的形式。这是 {@link #request()} 方法的异步版本。
     *
     * @param executorService 线程池
     * @param resultConsumer  结果消费器
     */
    void requestAsync(Executor executorService, Consumer<T> resultConsumer);

    /**
     * 同步请求并返回经典的 {@link ClassicHttpResponse} 实例。用户需要自行处理响应<b>并在使用完毕后关闭相关的资源。</b>
     * <p><b>需要特别注意的是， {@link Request#successCondition()} 不对此方法的请求过程生效。</b>
     * @return {@link ClassicHttpResponse} 实例
     */
    ClassicHttpResponse requestClassically() throws RequestFailedException;

    /**
     * {@link #requestClassically()} 的异步版本。
     *
     * @param executorService 线程池
     * @return 未来对象
     */
    CompletableFuture<ClassicHttpResponse> requestClassicallyAsync(Executor executorService);

    /**
     * {@link #requestClassically()} 的带回调异步版本。
     *
     * @param executorService 线程池
     * @param resultConsumer  回调函数
     */
    void requestClassicallyAsync(Executor executorService, Consumer<ClassicHttpResponse> resultConsumer);

    /**
     * 发起同步请求，在请求成功后获得一个可直接操作响应的 {@link OperableHttpResponse} 对象。
     *
     * @return {@link OperableHttpResponse} 对象
     */
    OperableHttpResponse requestOperable();

    /**
     * {@link #requestOperable()} 的异步版本。
     *
     * @param executorService 线程池
     * @return 未来对象
     */
    CompletableFuture<OperableHttpResponse> requestOperableAsync(Executor executorService);

    /**
     * {@link #requestOperable()} 的异步版本。
     *
     * @param executorService 线程池
     * @param resultConsumer  回调函数
     */
    void requestOperableAsync(Executor executorService, Consumer<OperableHttpResponse> resultConsumer);
}
