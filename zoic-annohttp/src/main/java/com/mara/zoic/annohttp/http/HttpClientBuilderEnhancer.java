package com.mara.zoic.annohttp.http;

import com.mara.zoic.annohttp.http.proxy.HttpConnectionSocketFactory;
import com.mara.zoic.annohttp.http.proxy.HttpsConnectionSocketFactory;
import com.mara.zoic.annohttp.http.proxy.RequestRoutePlanner;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;

import java.net.ProxySelector;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * {@link HttpClientBuilder} 增强器，为 ClientBuilder 添加代理支持。
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 */
public class HttpClientBuilderEnhancer {

    @SuppressWarnings("deprecation")
	static HttpClientBuilder enhance(HttpClientBuilder clientBuilder) {
        HttpsConnectionSocketFactory sslsf;
        try {
            SSLContextBuilder builder = null;
            builder = new SSLContextBuilder();
            // 全部信任 不做身份鉴定
            builder.loadTrustMaterial(null, (chain, authType) -> true);
            sslsf = new HttpsConnectionSocketFactory(builder.build(), new String[]{"SSLv3", "TLSv1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create http client", e);
        }
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new HttpConnectionSocketFactory())
                .register("https", sslsf)
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, null, null, TimeValue.of(15, TimeUnit.SECONDS));
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(2);
        clientBuilder
                .setRoutePlanner(new RequestRoutePlanner(ProxySelector.getDefault()))
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setProxyPreferredAuthSchemes(Arrays.asList(StandardAuthScheme.BASIC, StandardAuthScheme.NTLM, StandardAuthScheme.BEARER))
                        .setTargetPreferredAuthSchemes(Arrays.asList(StandardAuthScheme.BASIC, StandardAuthScheme.NTLM, StandardAuthScheme.BEARER)).build());

        return clientBuilder;
    }
}
