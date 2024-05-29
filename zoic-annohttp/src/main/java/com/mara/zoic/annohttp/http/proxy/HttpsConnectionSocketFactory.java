package com.mara.zoic.annohttp.http.proxy;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.BearerToken;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

/**
 * 用于HTTPS的连接套接字工厂。
 *
 * @author @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 */
@SuppressWarnings("deprecation")
public class HttpsConnectionSocketFactory extends SSLConnectionSocketFactory {

    private static final String REQUEST_PROXY_ID = RequestProxy.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpsConnectionSocketFactory.class);

    public HttpsConnectionSocketFactory(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        super(sslContext, hostnameVerifier);
    }

    public HttpsConnectionSocketFactory(SSLContext sslContext, String[] supportedProtocols, String[] supportedCipherSuites, HostnameVerifier hostnameVerifier) {
        super(sslContext, supportedProtocols, supportedCipherSuites, hostnameVerifier);
    }

    public HttpsConnectionSocketFactory(SSLContext sslContext) {
        super(sslContext);
    }

    public HttpsConnectionSocketFactory(SSLSocketFactory socketfactory, HostnameVerifier hostnameVerifier) {
        super(socketfactory, hostnameVerifier);
    }

    public HttpsConnectionSocketFactory(SSLSocketFactory socketFactory, String[] supportedProtocols, String[] supportedCipherSuites, HostnameVerifier hostnameVerifier) {
        super(socketFactory, supportedProtocols, supportedCipherSuites, hostnameVerifier);
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        Socket socket = null;
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
            socket = SocketFactory.getDefault().createSocket();
        }
        return socket;
    }

    @Override
    public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context)
            throws IOException {
        if (getRequestProxy(context) != null) {
            // 如果代理存在的话，让代理服务器去解析主机
            remoteAddress = InetSocketAddress.createUnresolved(host.getHostName(), host.getPort());
        }
        return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
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
