package com.mara.zoic.annohttp.http.proxy;


import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.BearerToken;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ProxySelector;

/**
 * 请求路由计划器。重写DefaultRoutePlanner用于探测自定义的代理设置。
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 */
@SuppressWarnings("deprecation")
public class RequestRoutePlanner extends SystemDefaultRoutePlanner {

    private static final String REQUEST_PROXY_ID = RequestProxy.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestRoutePlanner.class);

    public RequestRoutePlanner(ProxySelector proxySelector) {
        super(proxySelector);
    }

    @Override
    protected HttpHost determineProxy(HttpHost target, HttpContext context) throws HttpException {
        RequestProxy requestProxy = null;
        if (context instanceof HttpClientProxyContext) {
            requestProxy = ((HttpClientProxyContext) context).getRequestProxy();
        }
        if (requestProxy == null) {
            requestProxy = (RequestProxy) context.getAttribute(REQUEST_PROXY_ID);
        }
        if (requestProxy != null && requestProxy.getProxyType() == RequestProxy.ProxyType.HTTP) {
            // 这里只处理HTTP类型的代理
        	if (LOGGER.isDebugEnabled()) {
        		LOGGER.debug("Using proxy: " + requestProxy);
        	}
            if (requestProxy.withCredential()) {
                HttpClientContext httpClientContext = (HttpClientContext) context;
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                if (requestProxy.getProxyCredentialType() == RequestProxy.ProxyCredentialType.USERNAME_PASSWORD) {
                    credentialsProvider.setCredentials(new AuthScope(requestProxy.getHost(), requestProxy.getPort()),
                            new UsernamePasswordCredentials(requestProxy.getUserName(), requestProxy.getPassword().toCharArray()));
                } else if (requestProxy.getProxyCredentialType() == RequestProxy.ProxyCredentialType.WINDOWS_NT) {
                    credentialsProvider.setCredentials(new AuthScope(requestProxy.getHost(), requestProxy.getPort()),
                            new NTCredentials(requestProxy.getUserName(), requestProxy.getPassword().toCharArray(), requestProxy.getWorkstation(), requestProxy.getDomain()));
                } else if (requestProxy.getProxyCredentialType() == RequestProxy.ProxyCredentialType.BEARER_TOKEN) {
                    credentialsProvider.setCredentials(new AuthScope(requestProxy.getHost(), requestProxy.getPort()),
                            new BearerToken(requestProxy.getBearerToken()));
                }
                httpClientContext.setCredentialsProvider(credentialsProvider);
            }
            // 构造HttpHost返回
            return new HttpHost(requestProxy.getHost(), requestProxy.getPort());
        }
        return null;
    }

}
