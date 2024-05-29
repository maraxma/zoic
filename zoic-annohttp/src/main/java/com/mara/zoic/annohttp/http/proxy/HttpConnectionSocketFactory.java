package com.mara.zoic.annohttp.http.proxy;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.BearerToken;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于HTTP的连接套接字工厂。
 *
 * @author @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 */
@SuppressWarnings("deprecation")
public class HttpConnectionSocketFactory extends PlainConnectionSocketFactory {

    private static final String REQUEST_PROXY_ID = RequestProxy.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpConnectionSocketFactory.class);

	@Override
    public Socket createSocket(HttpContext context) throws IOException {
        Socket socket;
        RequestProxy requestProxy = getRequestProxy(context);
        if (requestProxy != null && requestProxy.getProxyType() == RequestProxy.ProxyType.SOCKS) {
            // 这里指只处理SOCKS类型的代理
            // 关于HTTP类型的代理由RequestRoutePlanner处理
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
            socket = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(requestProxy.getHost(), requestProxy.getPort())));
        } else {
            socket = new Socket();
        }
        return socket;
    }

    @Override
    public Socket connectSocket(TimeValue connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context)
            throws IOException {
        RequestProxy requestProxy = getRequestProxy(context);
        if (requestProxy != null && requestProxy.getProxyType() == RequestProxy.ProxyType.SOCKS) {
            // 如果代理存在的话，让代理服务器去解析主机
            remoteAddress = InetSocketAddress.createUnresolved(host.getHostName(), host.getPort());
        }
        return super.connectSocket(connectTimeout, sock, host, remoteAddress, localAddress, context);
    }

    private RequestProxy getRequestProxy(HttpContext context) {
        RequestProxy requestProxy = null;
        if (context instanceof HttpClientProxyContext) {
            requestProxy = ((HttpClientProxyContext) context).getRequestProxy();
        }
        if (requestProxy == null) {
            requestProxy = (RequestProxy) context.getAttribute(REQUEST_PROXY_ID);
        }
        return requestProxy;
    }

}
