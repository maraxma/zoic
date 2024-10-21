package com.mara.zoic.annohttp.http.proxy;

import org.apache.hc.client5.http.protocol.HttpClientContext;

/**
 * 带有代理设置的HTTP客户端上下文。
 * <p>这个类需要配合HttpConnectionSocketFactory和HttpsConnectionSocketFactory使用。</p>
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 */
public class HttpClientProxyContext extends HttpClientContext {

    public HttpClientProxyContext(RequestProxy requestProxy) {
        super();
        this.requestProxy = requestProxy;
    }

    private RequestProxy requestProxy;

    public RequestProxy getRequestProxy() {
        return requestProxy;
    }

    public void setRequestProxy(RequestProxy requestProxy) {
        this.requestProxy = requestProxy;
    }

}
