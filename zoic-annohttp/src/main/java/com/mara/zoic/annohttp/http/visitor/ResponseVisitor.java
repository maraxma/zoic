package com.mara.zoic.annohttp.http.visitor;


import com.mara.zoic.annohttp.annotation.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;

/**
 * 响应访问器。用于前置访问响应。
 * <p>用户可以自行实现此接口或者是 {@link BaseResponseVisitor} 类并配合{@link Request#responseVisitor()}来实现自己的前置访问逻辑。
 * <p>需要注意的是某些HttpResponse的content是不可重复的，除非你了解到是可重复的或者你有自己的打算，否则不要轻易在ResponseVisitor中获得Content，
 * 这样做会导致后面无法得到响应体。
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-10
 * @see Request#responseVisitor()
 */
public interface ResponseVisitor {

    /**
     * 访问响应。
     *
     * @param httpClientBuilder HttpClient建造者
     * @param httpClient        发起此次访问的HTTP客户端
     * @param serviceClient     发起此次访问的服务客户端
     * @param exception         发起请求过程中产生的错误，如果没有，这个参数是null
     * @param response          响应，如果存在exception，那么response是null
     * @throws Throwable 如果出现需要中断的需求，请抛出异常
     */
    void visit(HttpClientBuilder httpClientBuilder,
               CloseableHttpClient httpClient, Object serviceClient,
               ClassicHttpResponse response, Throwable exception) throws Throwable;
}
