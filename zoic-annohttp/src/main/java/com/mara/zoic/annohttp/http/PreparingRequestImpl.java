package com.mara.zoic.annohttp.http;


import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpTrace;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;

import com.mara.zoic.annohttp.annotation.AnnoHttpService;
import com.mara.zoic.annohttp.annotation.Body;
import com.mara.zoic.annohttp.annotation.FormField;
import com.mara.zoic.annohttp.annotation.FormFields;
import com.mara.zoic.annohttp.annotation.Headers;
import com.mara.zoic.annohttp.annotation.PathVar;
import com.mara.zoic.annohttp.annotation.PathVars;
import com.mara.zoic.annohttp.annotation.Proxy;
import com.mara.zoic.annohttp.annotation.Queries;
import com.mara.zoic.annohttp.annotation.Query;
import com.mara.zoic.annohttp.annotation.Request;
import com.mara.zoic.annohttp.annotation.Url;
import com.mara.zoic.annohttp.http.exception.NoApplicableResponseBodyConverterException;
import com.mara.zoic.annohttp.http.exception.RequestFailedException;
import com.mara.zoic.annohttp.http.exception.UnexpectedResponseException;
import com.mara.zoic.annohttp.http.protocol.ProtocolHandler;
import com.mara.zoic.annohttp.http.protocol.ProtocolHandlerMapping;
import com.mara.zoic.annohttp.http.proxy.HttpClientProxyContext;
import com.mara.zoic.annohttp.http.proxy.RequestProxy;
import com.mara.zoic.annohttp.http.request.converter.AutoRequestBodyConverter;
import com.mara.zoic.annohttp.http.request.converter.MapRequestBodyConverter;
import com.mara.zoic.annohttp.http.request.converter.RequestBodyConverterCache;
import com.mara.zoic.annohttp.http.response.converter.AutoResponseConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverterCache;
import com.mara.zoic.annohttp.http.spel.SpelUtils;
import com.mara.zoic.annohttp.http.visitor.ResponseVisitor;
import com.mara.zoic.annohttp.lifecycle.AnnoHttpLifecycle;
import com.mara.zoic.annohttp.testsup.PreparingRequestContainer;

non-sealed class PreparingRequestImpl<T> implements PreparingRequest<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreparingRequestImpl.class);

    /**
     * Spel的上下文。
     * <p>为了节省资源，真正发起请求的时候才创建。</p>
     */
    private EvaluationContext evaluationContext;

    /**
     * 请求方法的Method对象
     */
    private Method method;

    /**
     * 请求方法的实际参数
     */
    private Object[] args;

    protected Parameter[] parameters;

    protected Request requestAnno;

    protected String baseUrl;
    protected Function<HttpClientMetadata, String> baseUrlProvider;



    protected HttpMethod requestType;

    protected String url;

    protected HttpEntity httpEntity;
    protected RequestProxy requestProxy;

    protected List<CoverableNameValuePair> headers;
    protected List<CoverableNameValuePair> queries;
    protected Map<String, Object> formFields;
    protected Map<String, String> pathVars;
    protected HttpClientMetadata metadata;

    protected HttpClientBuilder userHttpClientBuilder;
    protected RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

    protected volatile CloseableHttpClient httpClient;
    protected static final Object HTTP_CLIENT_LOCK = new Object();

    PreparingRequestImpl(HttpClientMetadata metadata, Method method, Object[] args, String baseUrl, Function<HttpClientMetadata, String> baseUrlProvider) {

        this.metadata = metadata;
        this.method = method;
        this.args = args;
        this.baseUrl = baseUrl;
        this.baseUrlProvider = baseUrlProvider;

        parameters = method.getParameters();

        requestAnno = method.getAnnotation(Request.class);
        if (requestAnno == null) {
            throw new IllegalArgumentException("Method in an annohttp service client must be decorated by @Request");
        }

        AnnoHttpService annoHttpServiceAnno = method.getDeclaringClass().getAnnotation(AnnoHttpService.class);
        if (annoHttpServiceAnno != null) {
            if (this.baseUrl == null || this.baseUrl.isBlank()) {
                this.baseUrl = annoHttpServiceAnno.baseUrl();
            }
        }

        /*       1 处理HttpMethod  */
        processAndGenerateRequestMethod();

        /*       2 处理URL         */
        processAndGenerateUrl();

        /*       3 处理PathVars     */
        processAndGenerateRequestPathVars();

        /*       4 处理请求头       */
        // 注意优先级，参数请求头 > @Request注解请求头 > @ContentType请求头
        processAndGenerateRequestHeaders();

        /*       5 处理请参数       */
        // 注意优先级，参数列表中的查询参数 > @Request注解查询参数
        processAndGenerateRequestQueries();

        /*       6 处理代理设置     */
        processAndGenerateProxy();

        /*       7 处理Body      */
        // Body可以是各种类型的，annohttp会根据不同的类型采取不同的策略
        // Body只能有一个，且和@Field或者@Fields冲突，因为都会占用Body
        processAndGenerateEntity();

        /*    8 处理FormField */
        processAndGenerateFormFields();

        /* Test Support Code */
        processTestSupport();
    }

    private void processTestSupport() {
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            if (parameterType == PreparingRequestContainer.class) {
                PreparingRequestContainer preparingRequestContainer = (PreparingRequestContainer) args[i];
                if (preparingRequestContainer != null) {
                    preparingRequestContainer.setPreparingRequest(this);
                    break;
                }
            }
        }
    }

    private void processAndGenerateFormFields() {
        boolean bodyExisted = findAnnotation(parameters, Body.class);
        formFields = new LinkedHashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(FormField.class)) {
                if (parameters[i].isAnnotationPresent(FormFields.class)) {
                    throw new IllegalArgumentException("Only can use one of @FormField/@FormFields for one parameter");
                }
                if (bodyExisted) {
                    throw new IllegalArgumentException("@Body is exist, cannot use @FormField/@FormFields any more because they are occupy request body both");
                }
                FormField formFieldAnno = parameters[i].getAnnotation(FormField.class);
                String formFieldName = formFieldAnno.value();
                Object formFieldValue = args[i];
                checkFormField(formFieldName, formFieldValue);
                formFields.put(formFieldName, formFieldValue);
            } else if (parameters[i].isAnnotationPresent(FormFields.class)) {
                if (parameters[i].isAnnotationPresent(FormField.class)) {
                    throw new IllegalArgumentException("Only can use one of @FormField/@FormFields for one parameter");
                }
                if (bodyExisted) {
                    throw new IllegalArgumentException("@Body is exist, cannot use @FormField/@FormFields any more because they are occupy request body both");
                }
                if (!(args[i] instanceof Map)) {
                    throw new IllegalArgumentException("@FormFields can accept Map only");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) args[i];
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String formFieldName = entry.getKey();
                    Object formFieldValue = entry.getValue();
                    checkFormField(formFieldName, formFieldValue);
                    formFields.put(formFieldName, formFieldValue);
                }
            }
        }
    }

    private void processAndGenerateEntity() {
        ContentType computedRequestContentType = null;
        for (CoverableNameValuePair header : headers) {
            if (header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                computedRequestContentType = ContentType.parse(header.getValue());
                break;
            }
        }
        if (computedRequestContentType == null) {
            computedRequestContentType = ContentType.APPLICATION_JSON;
        }
        int bodyFound = 0;
        int bodyIndex = -1;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(Body.class)) {
                bodyFound++;
                bodyIndex = i;
            }
            if (bodyFound > 1) {
                throw new IllegalArgumentException("You cannot use more than 1 @Body");
            }
        }
        Class<?> requestBodyConverterClass = requestAnno.requestBodyConverter();
        if (bodyIndex != -1) {
            // 在参数列表中找到唯一的@Body
            Body bodyAnno = parameters[bodyIndex].getAnnotation(Body.class);
            httpEntity = convertRequestBody(requestBodyConverterClass, args[bodyIndex], computedRequestContentType, metadata, bodyAnno.value());
        } else {
            // 在参数列表中找不到Body才处理注解上的Body
            // 不要忘了在@Request中也有Body的设定
            String annoBodyString = requestAnno.bodyString();
            byte[] annoBodyBytes = requestAnno.bodyBytes();
            String annoBodySpel = requestAnno.bodySpel();
            if (!"".equals(annoBodyString)) {
                if (!"".equals(annoBodySpel) || annoBodyBytes.length != 0) {
                    throw new IllegalArgumentException("You can only use one of bodyString/bodyBytes/bodySpel to set body on @Request");
                }
                httpEntity = convertRequestBody(requestBodyConverterClass, annoBodyString, computedRequestContentType, metadata, DEFAULT_STRING_FIELD_NAME);
            } else if (annoBodyBytes.length != 0) {
                if (!"".equals(annoBodySpel)) {
                    throw new IllegalArgumentException("You can only use one of bodyString/bodyBytes/bodySpel to set body on @Request");
                }
                httpEntity = convertRequestBody(requestBodyConverterClass, annoBodyBytes, computedRequestContentType, metadata, DEFAULT_BYTES_FIELD_NAME);
            } else if (!"".equals(annoBodySpel)) {
                Object res = SpelUtils.executeSpel(annoBodySpel, evaluationContext, Object.class);
                httpEntity = convertRequestBody(requestBodyConverterClass, res, computedRequestContentType, metadata, DEFAULT_OBJECT_FIELD_NAME);
            }
        }
    }

    protected HttpEntity convertRequestBody(Class<?> requestBodyConverterClass, Object source,
                                            ContentType computedRequestContentType, HttpClientMetadata metadata, String formFieldName) {
        if (requestBodyConverterClass == AutoRequestBodyConverter.class) {
            return RequestBodyConverterCache.AUTO_REQUEST_BODY_CONVERTER.convert(source, computedRequestContentType, metadata, formFieldName);
        } else {
            if (!RequestBodyConverterCache.getAll().containsKey(requestBodyConverterClass)) {
                throw new IllegalStateException("The RequestBodyConverter '" + requestBodyConverterClass.getName() + "' cannot be found, please register it before using");
            }
            return RequestBodyConverterCache.getAll().get(requestBodyConverterClass).convert(source, computedRequestContentType, metadata, formFieldName);
        }
    }

    private void processAndGenerateProxy() {
        String requestProxySpel = requestAnno.proxy();
        int parameterProxyIndex = -1;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(Proxy.class)) {
                if (parameterProxyIndex != -1) {
                    // 已经找到了一个Proxy
                    throw new IllegalArgumentException("You cannot use more than 1 proxy represented by @Request.proxy or @Proxy)");
                } else {
                    parameterProxyIndex = i;
                }
            }
        }
        if (parameterProxyIndex != -1) {
            if (!"".equals(requestProxySpel)) {
                throw new IllegalArgumentException("You cannot use more than 1 proxy represented by @Request.proxy or @Proxy)");
            }
            if (!(RequestProxy.class.isAssignableFrom(parameters[parameterProxyIndex].getType()))) {
                throw new IllegalArgumentException("@Proxy must represent a RequestProxy type and you used type '" + parameters[parameterProxyIndex].getType().getName() + "'");
            }
            requestProxy = (RequestProxy) args[parameterProxyIndex];
        } else {
            if (!"".equals(requestProxySpel)) {
                Object requestProxySpelResult = SpelUtils.executeSpel(requestProxySpel, evaluationContext, Object.class);
                if (!(requestProxySpelResult instanceof RequestProxy)) {
                    throw new IllegalArgumentException("@Request.proxy must return an instance of RequestProxy and cannot be null");
                } else {
                    requestProxy = (RequestProxy) requestProxySpelResult;
                }
            }
        }
    }

    private void processAndGenerateUrl() {
        String computedUrl = requestAnno.url();
        String urlSpel = requestAnno.urlSpel();
        if (!"".equals(computedUrl) && !"".equals(urlSpel)) {
            throw new IllegalArgumentException("Only can set one of @Request.url and @Request.urlSpel");
        }
        if (!"".equals(urlSpel)) {
            Object res = SpelUtils.executeSpel(urlSpel, evaluationContext, Object.class);
            if (!(res instanceof String)) {
                throw new IllegalArgumentException("@Request.urlSpel must return a string instance");
            }
            computedUrl = (String) res;
        }
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            if (parameter.isAnnotationPresent(Url.class)) {
                if (!String.class.isAssignableFrom(parameterType)) {
                    throw new IllegalArgumentException("@Url accept String class only");
                }
                computedUrl = (String) args[i];
                break;
            }
        }

        String finalBaseUrl = baseUrl;
        if (baseUrlProvider != null) {
            finalBaseUrl = baseUrlProvider.apply(metadata);
        }
        if (finalBaseUrl != null && !finalBaseUrl.isBlank()) {
            url = concatUrl(finalBaseUrl, computedUrl);
            return;
        }

        url = computedUrl;
    }

    private static String concatUrl(String baseUrl, String subUrl) {
        if ((baseUrl.endsWith("/") && !subUrl.startsWith("/")) || (!baseUrl.endsWith("/") && subUrl.startsWith("/"))) {
            return baseUrl + subUrl;
        } else if (baseUrl.endsWith("/") && subUrl.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + subUrl;
        } else {
            return baseUrl + "/" + subUrl;
        }
    }

    @Override
    public PreparingRequest<T> customHttpClient(Supplier<HttpClientBuilder> httpClientBuilderSupplier) {
        if (httpClientBuilderSupplier != null) {
            HttpClientBuilder userHttpClientBuilder = httpClientBuilderSupplier.get();
            if (userHttpClientBuilder == null) {
                throw new IllegalArgumentException("httpClientBuilderSupplier must return an non-null HttpClientBuilder instance");
            }
            this.userHttpClientBuilder = userHttpClientBuilder;
        }
        return this;
    }

    @Override
    public PreparingRequest<T> customRequestHeaders(Consumer<List<CoverableNameValuePair>> headerConsumer) {
        if (headerConsumer != null) {
            headerConsumer.accept(headers);
        }
        return this;
    }

    private void processAndGenerateRequestHeaders() {
        if (headers == null) {
            headers = new LinkedList<>();
            boolean headerCoverable = requestAnno.headerCoverable();
            // 4.1 处理参数请求头
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Class<?> parameterType = parameter.getType();
                if (parameter.isAnnotationPresent(com.mara.zoic.annohttp.annotation.Header.class)) {
                    if (!String.class.isAssignableFrom(parameterType)) {
                        throw new IllegalArgumentException("@Header parameter should be type of String");
                    }
                    String argHeaderValue = (String) args[i];
                    if (null == argHeaderValue || argHeaderValue.isEmpty()) {
                        throw new IllegalArgumentException("arg for @Header cannot be null or empty: " + parameter.getName() + "(" + i + ")");
                    }
                    com.mara.zoic.annohttp.annotation.Header headerAnno = parameter.getAnnotation(com.mara.zoic.annohttp.annotation.Header.class);
                    String annoHeaderName = headerAnno.value();
                    if (annoHeaderName == null || (annoHeaderName = annoHeaderName.trim()).isEmpty()) {
                        throw new IllegalArgumentException("Header name cannot be null or empty: " + parameter.getName());
                    }
                    addCoverable(headers, new CoverableNameValuePair(annoHeaderName, argHeaderValue));
                }
                if (parameter.isAnnotationPresent(Headers.class)) {
                    if (!Map.class.isAssignableFrom(parameterType) && !String[].class.isAssignableFrom(parameterType)) {
                        throw new IllegalArgumentException("@Headers parameter should be type of String[] or Map<String, String>");
                    }
                    if (String[].class.isAssignableFrom(parameterType)) {
                        String[] headerArray = (String[]) args[i];
                        for (String s : headerArray) {
                            addCoverable(headers, getHeaderFromStringStyled(s, headerCoverable));
                        }
                    } else {
                        @SuppressWarnings("unchecked")
                        Map<String, String> headerMap = (Map<String, String>) args[i];
                        headerMap.forEach((k, v) -> {
                            checkHeader(k, v);
                            addCoverable(headers, new CoverableNameValuePair(k, v, headerCoverable));
                        });
                    }
                }
            }
            // 4.2 处理@Request中用户定义的请求头
            String[] annoHeaders = requestAnno.headers();
            String annoSpelHeaders = requestAnno.headersSpel();
            if (!"".equals(annoSpelHeaders) && annoHeaders.length != 0) {
                throw new IllegalArgumentException("You can only use one approach(headersSpel or headers) to set headers in @Request");
            } else {
                if (annoHeaders.length != 0) {
                    for (String annoHeader : annoHeaders) {
                        CoverableNameValuePair h = getHeaderFromStringStyled(annoHeader, headerCoverable);
                        addCoverable(headers, h);
                    }
                } else {
                    if (!annoSpelHeaders.isBlank()) {
                        Object spelRes = SpelUtils.executeSpel(annoSpelHeaders, evaluationContext, Object.class);
                        if (spelRes instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = ((Map<String, Object>) spelRes);
                            m.forEach((k, v) -> {
                                checkHeader(k, v);
                                addCoverable(headers, new CoverableNameValuePair(k, String.valueOf(v), false));
                            });
                        } else if (spelRes instanceof Object[] array) {
                            for (Object o : array) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m = ((Map<String, Object>) o);
                                String headerName = String.valueOf(m.get(MAP_KEY_NAME));
                                String headerValue = String.valueOf(m.get(MAP_KEY_VALUE));
                                boolean coverable = Boolean.parseBoolean(String.valueOf(m.get(MAP_KEY_COVERABLE)));
                                boolean headerInvalid = false;
                                if (null != headerName) {
                                    headerName = headerName.trim();
                                    if (headerName.isEmpty()) {
                                        headerInvalid = true;
                                    }
                                } else {
                                    headerInvalid = true;
                                }
                                if (null != headerValue) {
                                    headerValue = headerValue.trim();
                                } else {
                                    headerInvalid = true;
                                }
                                if (headerInvalid) {
                                    throw new IllegalArgumentException("Your header on @Request headerSpel is invalid, please check: " + m);
                                }
                                addCoverable(headers, new CoverableNameValuePair(headerName, headerValue, coverable));
                            }
                        }
                    }
                }
            }
            // 4.3 处理独有的注解式请求头
            processContentTypeAnnotation(headers, method, requestAnno);
            // 4.4 处理@Request.contentType()
            String annoContentType = requestAnno.contentType();
            if (!annoContentType.isBlank()) {
                headers.add(new CoverableNameValuePair(HttpHeaders.CONTENT_TYPE, annoContentType, headerCoverable));
            }
        }
    }

    protected void processContentTypeAnnotation(List<CoverableNameValuePair> existingHeaders, Method method, Request requestAnno) {
        String contentTypeAnnoName = null;
        for (Annotation annotation : method.getAnnotations()) {
            if ((contentTypeAnnoName = annotation.annotationType().getSimpleName()).startsWith("ContentType")) {
                break;
            }
            contentTypeAnnoName = null;
        }
        ContentType contentType;
        if (contentTypeAnnoName != null) {
            if (contentTypeAnnoName.contains("ApplicationJson")) {
                contentType = ContentType.APPLICATION_JSON;
            } else if (contentTypeAnnoName.contains("ApplicationXml")) {
                contentType = ContentType.APPLICATION_XML;
            } else if (contentTypeAnnoName.contains("ApplicationFormUrlEncoded")) {
                contentType = ContentType.APPLICATION_FORM_URLENCODED;
            } else if (contentTypeAnnoName.contains("TextPlain")) {
                contentType = ContentType.TEXT_PLAIN;
            } else if (contentTypeAnnoName.contains("ApplicationMultipartFormData")) {
                contentType = ContentType.MULTIPART_FORM_DATA;
            } else if (contentTypeAnnoName.contains("Wildcard")) {
                contentType = ContentType.WILDCARD;
            } else {
                // should never happen
                throw new IllegalArgumentException("Unsupported annotation ContentType: " + contentTypeAnnoName);
            }

            // 注意优先级，注解指定ContentType的优先级是最低的
            addCoverable(existingHeaders, new CoverableNameValuePair(HttpHeaders.CONTENT_TYPE, contentType.toString(), requestAnno.headerCoverable()));
        }
    }

    @Override
    public PreparingRequest<T> customRequestQueries(Consumer<List<CoverableNameValuePair>> queryConsumer) {
        if (queryConsumer != null) {
            queryConsumer.accept(queries);
        }
        return this;
    }

    private void processAndGenerateRequestQueries() {
        if (queries == null) {
            queries = new LinkedList<>();
            boolean queryCoverable = requestAnno.queryCoverable();
            // 5.1 处理方法参数列表中给出的请求参数
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Class<?> parameterType = parameter.getType();
                if (parameter.isAnnotationPresent(Query.class)) {
                    if (parameter.isAnnotationPresent(Queries.class)) {
                        throw new IllegalArgumentException("You can only use one of @Query & @Queries");
                    }
                    if (!String.class.isAssignableFrom(parameterType)) {
                        throw new IllegalArgumentException("@Query support String type only");
                    }
                    Query queryAnno = parameter.getAnnotation(Query.class);
                    String qKey = queryAnno.value();
                    if (qKey == null || qKey.isBlank()) {
                        throw new IllegalArgumentException("Query parameter's key cannot be null or empty");
                    }
                    addCoverable(queries, new CoverableNameValuePair(qKey, (String) args[i], queryCoverable));
                } else if (parameter.isAnnotationPresent(Queries.class)) {
                    if (parameter.isAnnotationPresent(Query.class)) {
                        throw new IllegalArgumentException("You can only use one of @Query & @Queries");
                    }
                    if (!Map.class.isAssignableFrom(parameterType)) {
                        throw new IllegalArgumentException("@Queries support Map<String, String> type only");
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, String> queryMap = (Map<String, String>) args[i];
                    for (Map.Entry<String, String> en : queryMap.entrySet()) {
                        String queryName = en.getKey();
                        String queryValue = en.getValue();
                        checkQueryParameter(queryName, queryValue);
                        addCoverable(queries, new CoverableNameValuePair(queryName, queryValue, queryCoverable));
                    }
                }
            }
            // 5.2 处理@Request.queries
            String[] annoQueries = requestAnno.queries();
            for (String q : annoQueries) {
                addCoverable(queries, getQueryParameterFromStringStyled(q, queryCoverable));
            }
            // 5.3 处理@Request.queriesSpel
            String queriesSpel = requestAnno.queriesSpel();
            if (!queriesSpel.isBlank()) {
                Object res = SpelUtils.executeSpel(queriesSpel, evaluationContext, Object.class);
                if (res instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> m = (Map<String, String>) res;
                    for (Map.Entry<String, String> entry : m.entrySet()) {
                        String queryName = entry.getKey();
                        String queryValue = entry.getValue();
                        checkQueryParameter(queryName, queryValue);
                        addCoverable(queries, new CoverableNameValuePair(queryName, queryValue));
                    }
                } else if (res instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> l = (List<Map<String, Object>>) res;
                    for (Map<String, Object> map : l) {
                        String queryName = (String) map.get("name");
                        String queryValue = (String) map.get("value");
                        checkQueryParameter(queryName, queryValue);
                        boolean coverable = Boolean.parseBoolean((String) map.get("coverable"));
                        addCoverable(queries, new CoverableNameValuePair(queryName, queryValue, coverable));
                    }
                }
            }
        }
    }

    @Override
    public PreparingRequest<T> customRequestPathVars(Consumer<Map<String, String>> pathVarConsumer) {
        if (pathVarConsumer != null) {
            pathVarConsumer.accept(pathVars);
        }
        return this;
    }

    private void processAndGenerateRequestPathVars() {
        if (pathVars == null) {
            pathVars = new HashMap<>();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Class<?> parameterType = parameter.getType();
                if (parameter.isAnnotationPresent(PathVar.class)) {
                    if (parameter.isAnnotationPresent(PathVars.class)) {
                        throw new IllegalArgumentException("A parameter can put one of PathVar/PathVars");
                    }
                    if (!String.class.isAssignableFrom(parameterType)) {
                        throw new IllegalArgumentException("@PathVar accept String class only");
                    }
                    NameValuePair nameValuePair = getPathVarFromStringStyled((String) args[i]);
                    pathVars.put(nameValuePair.getName(), nameValuePair.getValue());
                } else if (parameter.isAnnotationPresent(PathVars.class)) {
                    if (parameter.isAnnotationPresent(PathVar.class)) {
                        throw new IllegalArgumentException("A parameter can put one of PathVar/PathVars");
                    }
                    if (!Map.class.isAssignableFrom(parameterType)) {
                        throw new IllegalArgumentException("@PathVar accept Map class only(Map<String, String>)");
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, String> pathVarsMap = (Map<String, String>) args[i];
                    for (Map.Entry<String, String> entry : pathVarsMap.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        key = key == null ? null : key.trim();
                        value = value == null ? null : value.trim();
                        if (key == null || key.isEmpty()) {
                            throw new IllegalArgumentException("PathVar's name cannot be null or empty");
                        }
                        if (value == null || value.isEmpty()) {
                            throw new IllegalArgumentException("PathVar's value cannot be null or empty");
                        }
                        pathVarsMap.put(key, value);
                    }
                }
            }
        }
    }

    @Override
    public PreparingRequest<T> customRequestFormFields(Consumer<Map<String, Object>> formFieldConsumer) {
        if (formFieldConsumer != null) {
            formFieldConsumer.accept(formFields);
        }
        return this;
    }

    @Override
    public PreparingRequest<T> customRequestConfig(Consumer<RequestConfig.Builder> requestConfigBuilderConsumer) {
        if (requestConfigBuilderConsumer != null) {
            requestConfigBuilderConsumer.accept(requestConfigBuilder);
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T request() {
    	executeLifecycleBeforeRequestingMethod();
        ClassicHttpResponse httpResponse;
        try {
            httpResponse = executeRequest();
        } catch (Exception e) {
            throw new RequestFailedException("Request Failed for url " + url, e);
        }

        // 从这里开始便有了 httpResponse，出现任何异常应当释放 HttpResponse 里面的 Entity 所占用的资源
        Type userExpectedType = metadata.getRequestMethodActualType();
        try {
            Charset computedResponseCharset;
            ContentType computedResponseContentType;

            ContentType responseContentType = ContentType.parseLenient(httpResponse.getEntity().getContentType());
            Charset responseCharset = responseContentType == null ? null : responseContentType.getCharset();

            Request requestAnnotation = metadata.getRequestAnnotation();
            String userResponseContentType = requestAnnotation.responseContentType();
            boolean preferResponseContentType = requestAnnotation.preferUsingResponseContentType();

            String userResponseCharset = requestAnnotation.responseCharset();
            boolean preferResponseCharset = requestAnnotation.preferUsingResponseCharset();

            if (preferResponseContentType && responseContentType != null) {
                computedResponseContentType = responseContentType;
            } else {
                computedResponseContentType = ContentType.parse(userResponseContentType);
            }

            if (preferResponseCharset && responseCharset != null) {
                // 如果优先使用响应头中的charset，那么直接查找使用，如果未查找到，那么使用用户定义的charset
                computedResponseCharset = responseCharset;
            } else {
                // 否则直接使用用户定义的charset
                computedResponseCharset = Charset.forName(userResponseCharset);
            }

            ResponseConverter responseConverter;
            Class<? extends ResponseConverter> converterClass = metadata.getRequestAnnotation().responseConverter();
            if (converterClass != AutoResponseConverter.class) {
            	responseConverter = ResponseConverterCache.getAll().get(converterClass);
                if (responseConverter == null) {
                    throw new NoApplicableResponseBodyConverterException("Cannot find response convert '" + converterClass + "', please register it before using");
                }
            } else {
            	responseConverter = ResponseConverterCache.AUTO_RESPONSE_CONVERTER;
            }
            executeLifecycleAfterRequestedMethod(httpResponse, responseConverter);
            return (T) responseConverter.convert(httpResponse, metadata, computedResponseContentType, computedResponseCharset);
        } finally {
            // 出现或者不出现异常，视返回体的类型决定是否关闭资源
            // 目前只有两种类型的返回不能关闭资源 1) InputStream  2) HttpResponse
            if (userExpectedType instanceof @SuppressWarnings("rawtypes")Class clazz && (InputStream.class.isAssignableFrom(clazz) || ClassicHttpResponse.class.isAssignableFrom(clazz))) {
                // 不消费Entity
                LOGGER.warn("Using type '" + userExpectedType + "' as return type, you should close the HttpEntity/InputStream yourself");
            } else {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }
    }

    @Override
    public CompletableFuture<T> requestAsync(Executor executorService) {
        if (executorService == null) {
            throw new IllegalArgumentException("executorService cannot be null");
        }
        return CompletableFuture.supplyAsync(this::request, executorService);
    }

    @Override
    public void requestAsync(Executor executorService, Consumer<T> resultConsumer) {
        requestAsync(executorService).thenAccept(resultConsumer);
    }

    public CloseableHttpClient getHttpClient() {
        buildHttpClient();
        return httpClient;
    }

    @Override
    public PreparingRequest<T> customRequestUrl(Function<String, String> urlMapping) {
        if (urlMapping != null) {
            String userUrl = urlMapping.apply(url);
            if (userUrl == null || userUrl.isBlank()) {
                throw new IllegalArgumentException("Url cannot be null or empty");
            }
            url = userUrl;
        }
        return this;
    }

    @Override
    public PreparingRequest<T> customRequestProxy(Function<RequestProxy, RequestProxy> proxyMapping) {
        if (proxyMapping != null) {
            RequestProxy userProxy = proxyMapping.apply(requestProxy);
            if (userProxy != null) {
                requestProxy = userProxy;
            }
        }
        return this;
    }

    @Override
    public PreparingRequest<T> customHttpMethod(Function<HttpMethod, HttpMethod> httpMethodMapping) {
        if (httpMethodMapping != null) {
            HttpMethod userMethod = httpMethodMapping.apply(requestType);
            if (userMethod == null) {
                throw new IllegalArgumentException("HttpMethod cannot be null");
            }
            requestType = userMethod;
        }
        return this;
    }

    private void processAndGenerateRequestMethod() {
        requestType = requestAnno.method();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            if (parameter.isAnnotationPresent(com.mara.zoic.annohttp.annotation.Method.class)) {
                if (!HttpMethod.class.isAssignableFrom(parameterType)) {
                    throw new IllegalArgumentException("@Method accept HttpMethod class only");
                }
                requestType = (HttpMethod) args[i];
                break;
            }
        }
    }

    @Override
    public PreparingRequest<T> customRequestBody(Function<HttpEntity, HttpEntity> requestBodyMapping) {
        if (requestBodyMapping != null) {
            httpEntity = requestBodyMapping.apply(httpEntity);
        }
        return this;
    }

    @Override
    public ClassicHttpResponse requestClassically() throws RequestFailedException {
        return executeRequest();
    }

    @Override
    public CompletableFuture<ClassicHttpResponse> requestClassicallyAsync(Executor executorService) {
        return CompletableFuture.supplyAsync(this::requestClassically, executorService);
    }

    @Override
    public void requestClassicallyAsync(Executor executorService, Consumer<ClassicHttpResponse> resultConsumer) {
        requestClassicallyAsync(executorService).thenAccept(resultConsumer);
    }

    @Override
    public OperableHttpResponse requestOperable() {
        return new OperableHttpResponse(executeRequest());
    }

    @Override
    public CompletableFuture<OperableHttpResponse> requestOperableAsync(Executor executorService) {
        return CompletableFuture.supplyAsync(this::requestOperable, executorService);
    }

    @Override
    public void requestOperableAsync(Executor executorService,
                                     Consumer<OperableHttpResponse> resultConsumer) {
        requestOperableAsync(executorService).thenAccept(resultConsumer);
    }

    protected void buildHttpClient() {
        if (httpClient == null) {
            synchronized (HTTP_CLIENT_LOCK) {
                if (httpClient == null) {
                    if (userHttpClientBuilder != null) {
                        httpClient = userHttpClientBuilder.build();
                    } else {
                        httpClient = HttpComponentHolder.getHttpClientInstance();
                    }
                }
            }
        }
    }

    /**
     * 构建实际的空白请求对象（只包含计算出来的最终的URI）。
     *
     * @return @{@link HttpUriRequestBase} 实例
     */
    protected HttpUriRequestBase generateRawRequest() {

        String computedUrl = url;

        // 处理路径参数，必须完全匹配，要区分大小写
        if (pathVars != null) {
            for (Map.Entry<String, String> en : pathVars.entrySet()) {
                computedUrl = computedUrl.replace("{" + en.getKey() + "}", en.getValue());
            }
        }

        // 处理查询参数
        URI computedUri;
        if (queries != null) {
            List<NameValuePair> params = queries
                    .stream()
                    .map(e -> new BasicNameValuePair(e.getName(), e.getValue()))
                    .collect(Collectors.toList());
            try {
                computedUri = new URIBuilder(computedUrl).addParameters(params).build();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Cannot attach query parameters: " + queries + " to url " + computedUrl, e);
            }
        } else {
            computedUri = URI.create(computedUrl);
        }

        if (url == null || url.trim().isBlank()) {
            throw new IllegalArgumentException("Illegal url: " + url);
        }

        // 处理Method
        HttpUriRequestBase httpUriRequest;
        if (requestType == HttpMethod.GET) {
            httpUriRequest = new HttpGet(computedUri);
        } else if (requestType == HttpMethod.POST) {
            httpUriRequest = new HttpPost(computedUri);
        } else if (requestType == HttpMethod.DELETE) {
            httpUriRequest = new HttpDelete(computedUri);
        } else if (requestType == HttpMethod.OPTIONS) {
            httpUriRequest = new HttpOptions(computedUri);
        } else if (requestType == HttpMethod.PUT) {
            httpUriRequest = new HttpPut(computedUri);
        } else if (requestType == HttpMethod.TRACE) {
            httpUriRequest = new HttpTrace(computedUri);
        } else if (requestType == HttpMethod.HEAD) {
            httpUriRequest = new HttpHead(computedUri);
        } else if (requestType == HttpMethod.PATCH) {
            httpUriRequest = new HttpPatch(computedUri);
        } else {
            throw new IllegalArgumentException("Unsupported request type: " + requestType);
        }

        return httpUriRequest;
    }

    protected void executeProtocolHandler() {
        String protocol = url.substring(0, url.indexOf("://")).trim().toLowerCase();
        ProtocolHandler protocolHandler = ProtocolHandlerMapping.getHandler(protocol);
        protocolHandler.handle(metadata, this);
    }

    protected void fillEntityForRequest(HttpUriRequestBase httpUriRequest) {
        // 处理 Body 和 表单
        if (httpEntity != null) {
            if (formFields != null && !formFields.isEmpty()) {
                throw new IllegalArgumentException("You can only set body or formFields because they are occupy request body both");
            }
        } else {
            ContentType contentType = null;
            for (CoverableNameValuePair coverableNameValuePair : headers) {
                if (coverableNameValuePair.getName().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                    contentType = ContentType.parse(coverableNameValuePair.getValue());
                    break;
                }
            }
            if (formFields != null && !formFields.isEmpty()) {
                if (contentType == null) {
                    // 如果用户未设定Content-Type，那么自动设定
                    // 如果用户设定了，那么就要校验，在有Form的情况下，Content-Type只能设定为urlencoded或multipart-form-data
                    boolean otherTypeFound = formFields.values().stream().anyMatch(e -> !(e instanceof String));
                    if (otherTypeFound) {
                        contentType = ContentType.MULTIPART_FORM_DATA;
                        headers.add(new CoverableNameValuePair(HttpHeaders.CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA.toString()));
                    } else {
                        contentType = ContentType.APPLICATION_FORM_URLENCODED;
                        headers.add(new CoverableNameValuePair(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.toString()));
                    }
                } else {
                    if (!ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equalsIgnoreCase(contentType.getMimeType())
                            && !ContentType.MULTIPART_FORM_DATA.getMimeType().equalsIgnoreCase(contentType.getMimeType())) {
                        throw new IllegalArgumentException("If you are using @FormField or @FormFields, the content-type must be set to urlencoded or multipart-data. Or you can ignore content-type, annohttp will set it automatically");
                    }
                }
                httpEntity = RequestBodyConverterCache.getAll().get(MapRequestBodyConverter.class).convert(formFields,contentType, metadata, null);
            }
        }
        if (httpEntity != null) {
            httpUriRequest.setEntity(httpEntity);
        }
    }

    protected void fillHeadersForRequest(HttpUriRequestBase httpUriRequest) {
        // 处理请求头
        if (headers != null) {
            Header[] finalHeaders = headers
                    .stream()
                    .map(e -> new BasicHeader(e.getName(), e.getValue())).toList()
                    .toArray(new Header[0]);
            httpUriRequest.setHeaders(finalHeaders);
        }
    }

    protected void prepareRequestConfig() {
        // 处理超时设置
        int requestTimeout = metadata.getRequestTimeoutInSeconds();
        if (requestTimeout != -1) {
            requestConfigBuilder.setConnectionRequestTimeout(requestTimeout, TimeUnit.SECONDS);
        }
        int responseTimeout = metadata.getResponseTimeoutInSeconds();
        if (responseTimeout != -1) {
            requestConfigBuilder.setResponseTimeout(requestTimeout, TimeUnit.SECONDS);
        }

        // 处理disableRedirects
        if (metadata.disableRedirects()) {
            requestConfigBuilder.setRedirectsEnabled(false);
        }
    }

    protected void processAdditionalParameters(HttpClientMetadata metadata,
                                               HttpUriRequestBase httpUriRequest, RequestConfig.Builder requestConfigBuilder) {

    }

    /**
     * 执行请求并获得返回。
     *
     * @return {@link ClassicHttpResponse} 实例
     */
    protected ClassicHttpResponse executeRequest() {
        // 生成空白请求
        HttpUriRequestBase httpUriRequest = generateRawRequest();
        // 处理协议
        executeProtocolHandler();
        // 填充请求头
        fillHeadersForRequest(httpUriRequest);
        // 填充实体
        fillEntityForRequest(httpUriRequest);
        // 准备请求配置（还未正式设置进去）
        prepareRequestConfig();
        // 处理额外的参数（用户可写的）
        processAdditionalParameters(metadata, httpUriRequest, requestConfigBuilder);
        // 建立HttpClient
        buildHttpClient();
        ClassicHttpResponse httpResponse = null;
        Exception requestException = null;
        // 处理代理
        try {
            if (requestProxy != null) {
                // Must use executeOpen() to ensure a non-closed response entity(other execute methods will close the response entity automatically)
                httpResponse = httpClient.executeOpen(null, httpUriRequest, new HttpClientProxyContext(requestProxy));
            } else {
                httpResponse = httpClient.executeOpen(null, httpUriRequest, null);
            }
        } catch (IOException e) {
            requestException = e;
        }

        // 处理visitor
        executeResponseVisitor(httpResponse, requestException);

        // 处理successCondition，只有在注解驱动的HTTP客户端下才存在处理successCondition的判定
        processSuccessCondition(httpResponse);

        return httpResponse;
    }

    protected void processSuccessCondition(ClassicHttpResponse httpResponse) {
        if (metadata instanceof AnnoHttpClientMetadata) {
            String successCondition = metadata.getRequestAnnotation().successCondition();
            if (!successCondition.isBlank()) {
                EvaluationContext evaluationContext = SpelUtils.prepareSpelContext(metadata.getRequestMethodArguments());
                evaluationContext.setVariable("httpResponse", httpResponse);
                Object res = SpelUtils.executeSpel(successCondition, evaluationContext, Object.class);
                if (res instanceof Boolean b) {
                    if (!b) {
                        throw new UnexpectedResponseException("Unexpected response, the spel successCondition returns false: " + successCondition);
                    }
                } else {
                    throw new IllegalArgumentException("@Request.successCondition() must return a boolean value");
                }
            }
        }
    }

    protected void executeResponseVisitor(ClassicHttpResponse httpResponse, Exception requestException) {
        ResponseVisitor responseVisitor = metadata.getResponseVisitor();
        try {
            responseVisitor.visit(userHttpClientBuilder == null ? HttpComponentHolder.getHttpClientBuilderInstance() : userHttpClientBuilder, httpClient, this, httpResponse, requestException);
        } catch (Throwable e) {
            throw new RuntimeException("Something wrong with ResponseVisitor '" + responseVisitor + "'", e);
        }

        if (requestException != null) {
            throw new RuntimeException(requestException);
        }
    }
    
    protected void executeLifecycleBeforeRequestingMethod() {
    	for (AnnoHttpLifecycle lc : AnnoHttpLifecycleInstancesCahce.getAnnoHttpLifecycleInstances()) {
    		lc.beforeClientRequesting(metadata, this);
    	}
    }
    
    protected void executeLifecycleAfterRequestedMethod(HttpResponse response, ResponseConverter responseConverter) {
    	for (AnnoHttpLifecycle lc : AnnoHttpLifecycleInstancesCahce.getAnnoHttpLifecycleInstances()) {
    		lc.afterClientRequested(metadata, response, responseConverter);
    	}
    }

    private void addCoverable(List<CoverableNameValuePair> existing, CoverableNameValuePair incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Incoming coverable header/query parameter cannot be null");
        }
        Iterator<CoverableNameValuePair> iter = existing.iterator();
        while (iter.hasNext()) {
            CoverableNameValuePair existingCoverable = iter.next();
            if (((NameValuePair) existingCoverable).getName().equalsIgnoreCase(((NameValuePair) incoming).getName())) {
                if (existingCoverable.isCoverable()) {
                    iter.remove();
                }
            }
        }
        existing.add(incoming);
    }

    private CoverableNameValuePair getHeaderFromStringStyled(String stringStyledHeader, boolean coverable) {
        if (null == stringStyledHeader || stringStyledHeader.isBlank() || !stringStyledHeader.contains(":")) {
            throw new IllegalArgumentException("header string is invalid: " + stringStyledHeader);
        }
        int i = stringStyledHeader.indexOf(":");
        String headerName = stringStyledHeader.substring(0, i).trim();
        String headerValue = stringStyledHeader.substring(i + 1).trim();
        if ("".equals(headerName)) {
            throw new IllegalArgumentException("Header name '" + headerName + "' is invalid in header string '" + stringStyledHeader + "'");
        }
        return new CoverableNameValuePair(headerName, headerValue, coverable);
    }

    private void checkHeader(String headerName, Object headerValue) {
        if (null == headerName || headerName.isBlank() || headerValue == null) {
            throw new IllegalArgumentException("Header name cannot null or empty; headerValue cannot be null");
        }
    }

    private void checkQueryParameter(String queryName, String queryValue) {
        if (queryName == null || queryName.isBlank()) {
            throw new IllegalArgumentException("Query parameter's key cannot be null or empty");
        }
        if (queryValue == null) {
            throw new IllegalArgumentException("Query parameter's value cannot be null");
        }
    }

    private CoverableNameValuePair getQueryParameterFromStringStyled(String stringStyledQuery, boolean queryCoverable) {
        if (null == stringStyledQuery || stringStyledQuery.isBlank() || !stringStyledQuery.contains("=")) {
            throw new IllegalArgumentException("Query definition string is invalid: " + stringStyledQuery);
        }
        int i = stringStyledQuery.indexOf("=");
        String queryName = stringStyledQuery.substring(0, i).trim();
        String queryValue = stringStyledQuery.substring(i + 1).trim();
        if (queryName.isEmpty()) {
            throw new IllegalArgumentException("Query name '" + queryName + "' is invalid in query string '" + stringStyledQuery + "'");
        }
        return new CoverableNameValuePair(queryName, queryValue, queryCoverable);
    }

    private NameValuePair getPathVarFromStringStyled(String pathVar) {
        if (null == pathVar || pathVar.isBlank() || !pathVar.contains("=")) {
            throw new IllegalArgumentException("PathVar string is invalid: " + pathVar);
        }
        int i = pathVar.indexOf(":");
        String varName = pathVar.substring(0, i).trim();
        String varValue = pathVar.substring(i + 1).trim();
        if (varName.isEmpty()) {
            throw new IllegalArgumentException("PathVar name '" + varName + "' is invalid in pathVar string '" + pathVar + "'");
        }
        if (varValue.isEmpty()) {
            throw new IllegalArgumentException("PathVar value '" + varValue + "' is invalid in pathVar string '" + pathVar + "'");
        }
        return new BasicNameValuePair(varName, varValue);
    }

    private void checkFormField(String fieldName, Object fieldValue) {
        if (fieldName.isBlank()) {
            throw new IllegalArgumentException("Form field name cannot be empty");
        }
        if (fieldValue == null) {
            throw new IllegalArgumentException("Form field value cannot be null");
        }
    }

    private boolean findAnnotation(Parameter[] parameters, Class<? extends Annotation> annoClass) {
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(annoClass)) {
                return true;
            }
        }
        return false;
    }
}
