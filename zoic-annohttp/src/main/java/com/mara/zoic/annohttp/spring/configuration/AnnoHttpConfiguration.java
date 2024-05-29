package com.mara.zoic.annohttp.spring.configuration;

import com.mara.zoic.annohttp.http.proxy.HttpConnectionSocketFactory;
import com.mara.zoic.annohttp.http.proxy.HttpsConnectionSocketFactory;
import com.mara.zoic.annohttp.http.proxy.RequestRoutePlanner;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.ProxySelector;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@EnableConfigurationProperties({AnnoHttpProperties.class})
public class AnnoHttpConfiguration {

    @Bean
    public HttpClientBuilder httpClientBuilder(AnnoHttpProperties properties) {
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        if (properties.isTrustAnySsl()) {
            HttpsConnectionSocketFactory sslsf = null;
            try {
                SSLContextBuilder builder = null;
                builder = new SSLContextBuilder();
                // 全部信任 不做身份鉴定
                builder.loadTrustMaterial(null, (chain, authType) -> true);
                sslsf = new HttpsConnectionSocketFactory(builder.build(), new String[] {"SSLv3", "TLSv1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
            } catch (Exception e) {
                throw new RuntimeException("Cannot create http client", e);
            }
            socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", new HttpConnectionSocketFactory())
                    .register("https", sslsf)
                    .build();
        } else {
            socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", SSLConnectionSocketFactory.getSocketFactory())
                    .build();
        }
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, null, null, TimeValue.of(properties.getConnectionIdleTimeoutInSeconds(), TimeUnit.SECONDS));
        connectionManager.setMaxTotal(properties.getMaxConnections());
        connectionManager.setDefaultMaxPerRoute(properties.getMaxConnectionsPerRoute());
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.of(properties.getSocketTimeoutInSeconds(), TimeUnit.SECONDS))
                .build();
        connectionManager.setDefaultSocketConfig(socketConfig);
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(properties.getConnectTimeoutInSeconds(), TimeUnit.SECONDS))
                .build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        @SuppressWarnings("deprecation")
		HttpClientBuilder clientBuilder = HttpClients.custom()
                .setRoutePlanner(new RequestRoutePlanner(ProxySelector.getDefault()))
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setProxyPreferredAuthSchemes(Arrays.asList(StandardAuthScheme.BASIC, StandardAuthScheme.NTLM, StandardAuthScheme.BEARER))
                        .setTargetPreferredAuthSchemes(Arrays.asList(StandardAuthScheme.BASIC, StandardAuthScheme.NTLM, StandardAuthScheme.BEARER))
                        .setRedirectsEnabled(properties.isFlowRedirect())
                        .build());

        if (properties.isKeepAlive()) {
            clientBuilder
                    .setKeepAliveStrategy((HttpResponse response, HttpContext context) -> TimeValue.of(properties.getKeepAliveTimeInSeconds(), TimeUnit.SECONDS))
                    .setDefaultHeaders(Arrays.asList(new BasicHeader("Connection", "keep-alive"), new BasicHeader("Keep-Alive", "timeout=" + properties.getKeepAliveTimeInSeconds())));
        }
        return clientBuilder;
    }
}
