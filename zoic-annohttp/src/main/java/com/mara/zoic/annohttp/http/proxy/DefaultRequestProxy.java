package com.mara.zoic.annohttp.http.proxy;

import java.util.Objects;

/**
 * {@link RequestProxy} 的默认内部实现。
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-06
 */
class DefaultRequestProxy implements RequestProxy {

    String host;
    int port;
    ProxyType proxyType;
    String userName;
    boolean withCredential;
    String password;
    String domain;
    String workstation;
    ProxyCredentialType proxyCredentialType;
    private String bearerToken;

    /**
     * 构造一个代理类
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
    @SuppressWarnings("deprecation")
	DefaultRequestProxy(String host, int port, ProxyType proxyType,
                        boolean withCredential,
                        ProxyCredentialType proxyCredentialType,
                        String userName,
                        String password,
                        String domain,
                        String workstation,
                        String bearerToken
    ) {
        Objects.requireNonNull(host);
        Objects.requireNonNull(proxyType);
        if (withCredential) {
            // Default credential type is username/password
            if (proxyCredentialType == null) {
                proxyCredentialType = ProxyCredentialType.USERNAME_PASSWORD;
            }
            if (proxyCredentialType == ProxyCredentialType.BEARER_TOKEN) {
                Objects.requireNonNull(bearerToken);
            } else {
                Objects.requireNonNull(userName);
                Objects.requireNonNull(password);
                if (proxyCredentialType == ProxyCredentialType.WINDOWS_NT) {
                    Objects.requireNonNull(domain);
                    Objects.requireNonNull(workstation);
                }
            }
        }
        this.host = host;
        this.port = port;
        this.proxyType = proxyType;
        this.userName = userName;
        this.withCredential = withCredential;
        this.password = password;
        this.domain = domain;
        this.workstation = workstation;
        this.proxyCredentialType = proxyCredentialType;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public boolean withCredential() {
        return withCredential;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public String getWorkstation() {
        return workstation;
    }

    @Override
    public ProxyCredentialType getProxyCredentialType() {
        return proxyCredentialType;
    }

    @Override
    public String getBearerToken() {
        return bearerToken;
    }

    @Override
    public ProxyType getProxyType() {
        return proxyType;
    }
}
