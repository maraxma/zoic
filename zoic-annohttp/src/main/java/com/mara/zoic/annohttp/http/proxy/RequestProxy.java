package com.mara.zoic.annohttp.http.proxy;

/**
 * 代表一个发起请求需要的代理。
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 */
public interface RequestProxy {

    String getHost();

    int getPort();

    String getUserName();

    boolean withCredential();

    String getPassword();

    String getDomain();

    String getWorkstation();

    ProxyCredentialType getProxyCredentialType();

    String getBearerToken();

    ProxyType getProxyType();

    enum ProxyCredentialType {

        /**
         * 没有验证
         */
        NONE,

        /**
         * Window NT验证方式，需要用户名、密码、域、工作站
         * @deprecated 不再推荐使用这种验证方式，因为其不再被HttpClient继续支持，后续可能会变得不安全。推荐使用 {@link #BEARER_TOKEN}方式。
         */
        WINDOWS_NT,

        /**
         * 普通用户名密码验证方式
         */
        USERNAME_PASSWORD,

        /**
         * BearerToken
         */
        BEARER_TOKEN
    }

    enum ProxyType {

        /**
         * HTTP协议的代理
         */
        HTTP,

        /**
         * SOCKS代理
         */
        SOCKS
    }

    /**
     * 创建一个代理类。
     *
     * @param host                主机
     * @param port                端口
     * @param proxyType           代理的类型
     * @param withCredential      是否包含验证
     * @param proxyCredentialType 验证类型
     * @param userName            用户名
     * @param password            密码
     * @param domain              域（NT验证才需要，若非NT验证则可以传入null）
     * @param workstation         工作站（NT验证才需要，若非NT验证则可以传入null）
     */
    static RequestProxy create(String host, int port, ProxyType proxyType,
                               boolean withCredential,
                               ProxyCredentialType proxyCredentialType,
                               String userName,
                               String password,
                               String domain,
                               String workstation,
                               String bearerToken) {
        return new DefaultRequestProxy(host, port, proxyType, withCredential, proxyCredentialType, userName, password, domain, workstation, bearerToken);
    }

    /**
     * 创建一个新的 {@link RequestProxy} 拷贝。
     *
     * @param requestProxy 原对象
     * @return 新的对象
     */
    static RequestProxy newCopy(RequestProxy requestProxy) {
        return new DefaultRequestProxy(requestProxy.getHost(), requestProxy.getPort(), requestProxy.getProxyType(), requestProxy.withCredential(), requestProxy.getProxyCredentialType(), requestProxy.getUserName(), requestProxy.getPassword(), requestProxy.getDomain(), requestProxy.getWorkstation(), requestProxy.getBearerToken());
    }
}
