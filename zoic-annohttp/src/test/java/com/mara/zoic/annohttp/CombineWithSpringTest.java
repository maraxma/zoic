package com.mara.zoic.annohttp;

import com.mara.zoic.annohttp.httpservice.TestClient;
import com.mara.zoic.annohttp.httpservice.TestClientWithBaseUriFunction;
import com.mara.zoic.annohttp.httpservice.TestClientWithoutAnno;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest(classes = SpringTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class CombineWithSpringTest {

    @Autowired(required = false)
    private TestClient testClient;

    @Autowired(required = false)
    private TestClientWithoutAnno testClientWithoutAnno;

    @Autowired(required = false)
    private TestClientWithBaseUriFunction testClientWithBaseUriFunction;

    static HttpServer httpServer;
    static HttpServer httpServer2;

    @BeforeAll
    static void beforeAll() {

        /*
         * 新建一个HTTP服务，请求什么就响应什么，方便测试
         */

        final var vertx = Vertx.vertx();
        final var router = Router.router(vertx);
        router.route("/test").handler(rctx -> {
            var request = rctx.request();
            var requestHeaders = request.headers();
            var requestMethod = request.method();
            var requestParam = request.params();
            var response = rctx.response();
            requestHeaders.forEach(entry -> response.headers().add(entry.getKey(), entry.getValue()));
            response.putHeader("Request-Method", requestMethod.name());
            response.putHeader("Request-URI", request.absoluteURI());
            requestParam.forEach(entry -> response.putHeader("Request-Param-" + entry.getKey(), entry.getValue()));
            request.body(r -> response.end(r.result()));
        });
        httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router)
                .listen(8081).onSuccess(r -> System.out.println("已开启HTTP服务：" + r.actualPort())).result();

        final var router2 = Router.router(vertx);
        router2.route("/test").handler(rctx -> {
            var request = rctx.request();
            var requestHeaders = request.headers();
            var requestMethod = request.method();
            var requestParam = request.params();
            var response = rctx.response();
            requestHeaders.forEach(entry -> response.headers().add(entry.getKey(), entry.getValue()));
            response.putHeader("Request-Method", requestMethod.name());
            response.putHeader("Request-URI", request.absoluteURI());
            requestParam.forEach(entry -> response.putHeader("Request-Param-" + entry.getKey(), entry.getValue()));
            request.body(r -> response.end(r.result()));
        });
        httpServer2 = vertx.createHttpServer();
        httpServer2.requestHandler(router)
                .listen(9081).onSuccess(r -> System.out.println("已开启HTTP服务：" + r.actualPort())).result();
    }

    @Test
    void scanTest() {
        Assertions.assertNotNull(testClient);
        Assertions.assertNull(testClientWithoutAnno);
    }

    @Test
    void testBaseUri() {
    	Header[] headers = testClientWithBaseUriFunction.getItemName("123");
        String requestUri = Arrays.stream(headers).filter(h -> h.getName().equals("Request-URI")).map(Header::getValue).findFirst().orElseGet(() -> null);
        Assertions.assertNotNull(requestUri);
        Assertions.assertEquals("http://localhost:8081/test?ItemNo=123", requestUri);
    }
    
    @Test
    void testLifecycle() {
    	Header[] headers = testClientWithBaseUriFunction.getItemName("123");
        String hd = Arrays.stream(headers).filter(h -> h.getName().equals("X-Lifecycle-Spring")).map(Header::getValue).findFirst().orElseGet(() -> null);
        Assertions.assertNotNull(hd);
        Assertions.assertEquals("Added", hd);
    }

    void testBaseUriFunction() {
        Header[] headers = testClientWithBaseUriFunction.getItemName("123");
        String requestUri = Arrays.stream(headers).filter(h -> h.getName().equals("Request-URI")).map(Header::getValue).findFirst().orElseGet(() -> null);
        Assertions.assertNotNull(requestUri);
        Assertions.assertEquals("http://localhost:8081/test?ItemNo=123", requestUri);

        Header[] headers2 = testClientWithBaseUriFunction.getItemName("Special");
        String requestUri2 = Arrays.stream(headers2).filter(h -> h.getName().equals("Request-URI")).map(Header::getValue).findFirst().orElseGet(() -> null);
        Assertions.assertNotNull(requestUri2);
        Assertions.assertEquals("http://localhost:9081/test?ItemNo=Special", requestUri2);
    }
}
