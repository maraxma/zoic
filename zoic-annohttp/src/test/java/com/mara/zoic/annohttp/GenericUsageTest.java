package com.mara.zoic.annohttp;


import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.StatusLine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.mara.zoic.annohttp.annotation.Body;
import com.mara.zoic.annohttp.annotation.ContentTypeTextPlain;
import com.mara.zoic.annohttp.annotation.FormField;
import com.mara.zoic.annohttp.annotation.FormFields;
import com.mara.zoic.annohttp.annotation.Header;
import com.mara.zoic.annohttp.annotation.Headers;
import com.mara.zoic.annohttp.annotation.Method;
import com.mara.zoic.annohttp.annotation.Queries;
import com.mara.zoic.annohttp.annotation.Query;
import com.mara.zoic.annohttp.annotation.Request;
import com.mara.zoic.annohttp.annotation.Uri;
import com.mara.zoic.annohttp.http.AnnoHttpClients;
import com.mara.zoic.annohttp.http.CoverableNameValuePair;
import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.HttpMethod;
import com.mara.zoic.annohttp.http.PreparingRequest;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;
import com.mara.zoic.annohttp.lifecycle.AnnoHttpLifecycle;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

/**
 * 展示 annothttp 常规用法的测试类。
 * @author MARA.X.Ma
 * @since 1.0.0 2021-1-21
 */
public class GenericUsageTest {

    static HttpServer httpServer;

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
    }

    @AfterAll
    static void afterAll() {
        if (httpServer != null) {
            httpServer.close(result -> {
                System.out.println("已关闭HTTP服务器");
                result.result();
            });
        }
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，默认JSON请求类型，默认UTF-8，返回JSON字符串")
    void baseTest() {
        interface Client {
            @Request(uri = "http://localhost:8081/test")
            String baseRequest(@Body String jsonBody);
        }

        Client c = AnnoHttpClients.create(Client.class);

        String req =  """
                {
                    "Name": "Mara"
                }
                """;

        String resp = c.baseRequest(req);

        // 请求体 = 响应体
        Assertions.assertEquals(req, resp);
    }
    
    @Test
    @DisplayName("普通测试 -- POST方式，直接发送HttpEntity")
    void baseTest_2() {
        interface Client {
            @Request(uri = "http://localhost:8081/test")
            String baseRequest(HttpEntity httpEntity);
        }

        Client c = AnnoHttpClients.create(Client.class);

        String req =  """
                {
                    "Name": "Mara"
                }
                """;
        HttpEntity entity = EntityBuilder.create().setText(req).setContentType(ContentType.APPLICATION_JSON).setContentEncoding("UTF-8").build();
        String resp = c.baseRequest(entity);

        // 请求体 = 响应体
        Assertions.assertEquals(req, resp);
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，自定义ContentType")
    void baseTest2() {
        interface Client {
            @Request(uri = "http://localhost:8081/test")
            @ContentTypeTextPlain
            ClassicHttpResponse baseRequest(@Body String jsonBody);
        }

        Client c = AnnoHttpClients.create(Client.class);

        String req =  """
                {
                    "Name": "Mara"
                }
                """;

        ClassicHttpResponse resp = c.baseRequest(req);

        // 请求体 = 响应体
        try {
            Assertions.assertEquals(req, IOUtils.toString(resp.getEntity().getContent(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 请求头Content-Type = PainText
        org.apache.hc.core5.http.Header[] headers = resp.getHeaders("Content-Type");
        Assertions.assertEquals(1, headers.length);
        Assertions.assertEquals("text/plain; charset=ISO-8859-1", headers[0].getValue());
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，直接以JSON转换到Bean")
    void baseTest3() {

        record Bean(String name) {

        }

        interface Client {
            @Request(uri = "http://localhost:8081/test")
            Bean baseRequest(@Body String jsonBody);
        }

        Client c = AnnoHttpClients.create(Client.class);

        String req =  """
                {
                    "name": "Mara"
                }
                """;

        Bean resp = c.baseRequest(req);

        Assertions.assertEquals("Mara", resp.name);
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，间接以JSON转换到Bean")
    void baseTest4() {

        record Bean(String name) {

        }

        interface Client {
            @Request(uri = "http://localhost:8081/test")
            PreparingRequest<Bean> baseRequest(@Body String jsonBody);
        }

        Client c = AnnoHttpClients.create(Client.class);

        String s =  """
                {
                    "name": "Mara"
                }
                """;

        PreparingRequest<Bean> req = c.baseRequest(s);
        try (var operableHttpResponse = req.requestOperable()) {
            Assertions.assertEquals("Mara", operableHttpResponse.asJsonConvertible().toBean(Bean.class).name);
        }
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，直接转换为Map")
    void baseTest5() {

        interface Client {
            @Request(uri = "http://localhost:8081/test")
            Map<String, Object> baseRequest(@Body String jsonBody);
        }

        Client c = AnnoHttpClients.create(Client.class);

        String s =  """
                {
                    "name": "Mara"
                }
                """;

        Map<String, Object> map = c.baseRequest(s);

        Assertions.assertEquals("Mara", map.get("name"));
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，自定义请求头，获得所有响应头")
    void baseTest6() {

        interface Client {
            @Request(uri = "http://localhost:8081/test", headers = {"Mara: 1", "Mara: 2"})
            org.apache.hc.core5.http.Header[] baseRequest(@Body String jsonBody);
        }

        Client c = AnnoHttpClients.create(Client.class);

        String s =  """
                {
                    "name": "Mara"
                }
                """;

        org.apache.hc.core5.http.Header[] headers = c.baseRequest(s);

        List<org.apache.hc.core5.http.Header> headerList = Arrays.stream(headers).filter(e -> e.getName().equals("Mara")).toList();

        Assertions.assertEquals(2, headerList.size());
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，不附加Body，期望不附加ContentType")
    void baseTest7() {

        interface Client {
            @Request(uri = "http://localhost:8081/test", headers = {"Mara: 1", "Mara: 2"})
            org.apache.hc.core5.http.Header[] baseRequest();
        }

        Client c = AnnoHttpClients.create(Client.class);

        org.apache.hc.core5.http.Header[] headers = c.baseRequest();

        List<org.apache.hc.core5.http.Header> headerList = Arrays.stream(headers).filter(e -> e.getName().equals("Content-Type")).toList();

        Assertions.assertEquals(0, headerList.size());
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，只获取StatusLine")
    void baseTest8() {

        interface Client {
            @Request(uri = "http://localhost:8081/test", successCondition = "true")
            StatusLine baseRequest();
        }

        Client c = AnnoHttpClients.create(Client.class);

        StatusLine statusLine = c.baseRequest();

        Assertions.assertEquals(200, statusLine.getStatusCode());
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，通过PreparingRequest获取StatusLine")
    void baseTest9() {

        interface Client {
            @Request(uri = "http://localhost:8081/test", successCondition = "")
            PreparingRequest<StatusLine> baseRequest();
            // 不推荐将StatusLine、Header[]等特殊类型放入PreparingRequest，因为PreparingRequest是专门处理响应体的
        }

        Client c = AnnoHttpClients.create(Client.class);
        PreparingRequest<StatusLine> preparingRequest = c.baseRequest();

        HttpResponse httpResponse = preparingRequest.requestClassically();
        Assertions.assertEquals(200, httpResponse.getCode());

        StatusLine statusLine = preparingRequest.request();
        Assertions.assertEquals(200, statusLine.getStatusCode());

        // 断言抛出错误，因为无法将响应体转换为StatusLine
        try (final var o = preparingRequest.requestOperable()) {
            Assertions.assertThrows(Exception.class, () -> o.asJavaSerializedSequenceToObject(StatusLine.class));
        }
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，附加baseUri")
    void baseTest10() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            StatusLine baseRequest();
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        StatusLine statusLine = c.baseRequest();
        Assertions.assertEquals(200, statusLine.getStatusCode());

        Client c2 = AnnoHttpClients.create(Client.class, (metadata -> {
            if (metadata.getRequestAnnotation().method() == HttpMethod.GET) {
                return "http://localhost:8081/";
            } else {
                return "http://localhost:9081/";
            }
        }));
        StatusLine statusLine2 = c2.baseRequest();
        Assertions.assertEquals(200, statusLine2.getStatusCode());
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Headers附加请求头(使用Map)")
    void baseTest11() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Headers Map<String, String> headers);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest(Map.of("1", "1", "2", "2"));

        HashMap<String, String> headerMap = Arrays.stream(headers).filter(header -> "1".equals(header.getName()) || "2".equals(header.getName()))
                .collect(HashMap::new, (l, r) -> l.put(r.getName(), r.getValue()), HashMap::putAll);

        Assertions.assertEquals(2, headerMap.size());
        Assertions.assertEquals("1", headerMap.get("1"));
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Headers附加请求头(使用String[])")
    void baseTest12() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Headers String[] headers);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest(new String[] { "1: 1", "2: 2" });

        HashMap<String, String> headerMap = Arrays.stream(headers).filter(header -> "1".equals(header.getName()) || "2".equals(header.getName()))
                .collect(HashMap::new, (l, r) -> l.put(r.getName(), r.getValue()), HashMap::putAll);

        Assertions.assertEquals(2, headerMap.size());
        Assertions.assertEquals("1", headerMap.get("1"));
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Header附加请求头")
    void baseTest13() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Header("Token") String token, @Header("Token2") String token2);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest("Fake token", "Fake token 2");

        HashMap<String, String> headerMap = Arrays.stream(headers)
                .collect(HashMap::new, (l, r) -> l.put(r.getName(), r.getValue()), HashMap::putAll);

        Assertions.assertEquals("Fake token", headerMap.get("Token"));
        Assertions.assertEquals("Fake token 2", headerMap.get("Token2"));
    }
    
    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中直接使用Header附加请求头")
    void baseTest13_2() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(org.apache.hc.core5.http.Header header1, org.apache.hc.core5.http.Header header2);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest(new BasicHeader("Token", "Fake token"), new BasicHeader("Token2", "Fake token 2"));

        HashMap<String, String> headerMap = Arrays.stream(headers)
                .collect(HashMap::new, (l, r) -> l.put(r.getName(), r.getValue()), HashMap::putAll);

        Assertions.assertEquals("Fake token", headerMap.get("Token"));
        Assertions.assertEquals("Fake token 2", headerMap.get("Token2"));
    }
    
    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中直接使用Header[]附加请求头")
    void baseTest13_3() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(org.apache.hc.core5.http.Header[] headers);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest(new org.apache.hc.core5.http.Header[] {new BasicHeader("Token", "Fake token"), new BasicHeader("Token2", "Fake token 2")});

        HashMap<String, String> headerMap = Arrays.stream(headers)
                .collect(HashMap::new, (l, r) -> l.put(r.getName(), r.getValue()), HashMap::putAll);

        Assertions.assertEquals("Fake token", headerMap.get("Token"));
        Assertions.assertEquals("Fake token 2", headerMap.get("Token2"));
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，通过@FormFields发送表单(使用Map)")
    void baseTest14() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            String baseRequest(@FormFields Map<String, String> formFields);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("1", "1");
        linkedHashMap.put("2", "2");
        String body = c.baseRequest(linkedHashMap);

        Assertions.assertEquals("1=1&2=2", body);
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，通过@FormField发送表单")
    void baseTest15() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            String baseRequest(@FormField("Name") String name, @FormField("Age") String age);

            @Request(uri = "/test", successCondition = "true", contentType = "application/x-www-form-urlencoded")
            String baseRequest2(@FormField("Name") String name, @FormField("Age") int age);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        String body = c.baseRequest("Mara", "18");
        Assertions.assertEquals("Name=Mara&Age=18", body);

        String body2 = c.baseRequest2("Mara", 18);
        Assertions.assertEquals("Name=Mara&Age=18", body2);

        // PS: 如果@FormField全部都是String，那么会按照urlencoded发送， 只要有一个不是，那么会按照multi-data发送
        // 当然亦可以自行指定content-type
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Queries附加查询参数")
    void baseTest16() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Queries Map<String, String> queries);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest(Map.of("Name", "Mara", "Age", "18"));

        HashMap<String, String> headerMap = Arrays.stream(headers)
                .filter(e -> e.getName().startsWith("Request-Param-"))
                .collect(HashMap::new, (l, r) -> l.put(r.getName(), r.getValue()), HashMap::putAll);

        Assertions.assertEquals("Mara", headerMap.get("Request-Param-Name"));
        Assertions.assertEquals("18", headerMap.get("Request-Param-Age"));
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Query附加查询参数")
    void baseTest17() {

        interface Client {
            @Request(uri = "/test", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Query("Name") String name, @Query("Age") String age);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest("Mara", "18");

        HashMap<String, String> headerMap = Arrays.stream(headers)
                .filter(e -> e.getName().startsWith("Request-Param-"))
                .collect(HashMap::new, (l, r) -> l.put(r.getName(), r.getValue()), HashMap::putAll);

        Assertions.assertEquals("Mara", headerMap.get("Request-Param-Name"));
        Assertions.assertEquals("18", headerMap.get("Request-Param-Age"));
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Uri指定Uri")
    void baseTest18() {

        interface Client {
            @Request(successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Uri String uri);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest("/test");
        
        Optional<org.apache.hc.core5.http.Header> hd = Arrays.stream(headers).filter(h -> h.getName().equals("Request-URI")).findFirst();

        Assertions.assertTrue(hd.isPresent());
        Assertions.assertEquals("http://localhost:8081/test", hd.get().getValue());
    }
    
    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中直接使用URI")
    void baseTest18_2() {

    	interface Client {
            @Request(successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(URI uri);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest(URI.create("/test"));
        
        Optional<org.apache.hc.core5.http.Header> hd = Arrays.stream(headers).filter(h -> h.getName().equals("Request-URI")).findFirst();

        Assertions.assertTrue(hd.isPresent());
        Assertions.assertEquals("http://localhost:8081/test", hd.get().getValue());
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Method指定method")
    void baseTest19() {

        interface Client {
            @Request(uri = "/testXXX", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Uri String uri, @Method HttpMethod method);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest("/test", HttpMethod.POST);

        Assertions.assertEquals("POST", Arrays.stream(headers).filter(e -> "Request-Method".equals(e.getName())).findFirst().map(org.apache.hc.core5.http.Header::getValue).orElse(null));
    }
    
    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中直接使用HttpMethod指定Method")
    void baseTest19_2() {

        interface Client {
            @Request(uri = "/testXXX", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Uri String uri, HttpMethod method);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest("/test", HttpMethod.POST);

        Assertions.assertEquals("POST", Arrays.stream(headers).filter(e -> "Request-Method".equals(e.getName())).findFirst().map(org.apache.hc.core5.http.Header::getValue).orElse(null));
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用多个@Body，预期抛出异常")
    void baseTest20() {

        interface Client {
            @Request(uri = "/testXXX", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Uri String uri, @Method HttpMethod method, @Body Map<String, Object> body1, @Body Map<String, Object> body2);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        Executable executable =  () -> c.baseRequest("/test", HttpMethod.POST, Map.of(), Map.of());

        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("You cannot use more than 1 @Body in argument list", e.getMessage());
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，使用void作为返回")
    void baseTest21() {

        interface Client {
            @Request(uri = "/testXXX", successCondition = "true")
            void baseRequest(@Uri String uri, @Method HttpMethod method, @Body Map<String, Object> body);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        c.baseRequest("/test", HttpMethod.POST, Map.of());

        interface Client2 {
            @Request(uri = "/testXXX", successCondition = "true")
            PreparingRequest<Void> baseRequest(@Uri String uri, @Method HttpMethod method, @Body Map<String, Object> body);
        }
        Client2 c2 = AnnoHttpClients.create(Client2.class, "http://localhost:8081/");
        PreparingRequest<Void> pr2 = c2.baseRequest("/test", HttpMethod.POST, Map.of());
        Assertions.assertEquals(null, pr2.request());
    }
    
    @Test
    @DisplayName("普通测试 -- 默认GET方式，使用生命周期接口")
    void baseTest22() {
    	interface Client {
            @Request(uri = "/testXXX", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Uri String uri, @Method HttpMethod method, @Body Map<String, Object> body);
        }
    	class LifecycleDemo implements AnnoHttpLifecycle {

    		@Override
    		public void beforeClientCreating(Class<?> clientClass) {

    		}

    		@Override
    		public void afterClientCreated(Object client) {

    		}

    		@Override
    		public void beforeClientRequesting(HttpClientMetadata httpClientMetadata, PreparingRequest<?> preparingRequest) {
    			if (httpClientMetadata.getServiceClientClass() == Client.class) {
    				preparingRequest.customRequestHeaders(t -> t.add(new CoverableNameValuePair("X-Lifecycle", "Added")));
    			}
    		}

    		@Override
    		public void afterClientRequested(HttpClientMetadata httpClientMetadata, ClassicHttpResponse httpResponse,
    				ResponseConverter responseConverter) {

    		}
    	}
    	
    	Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
    	AnnoHttpClients.addAnnoHttpLifecycleInstances(new LifecycleDemo());
        org.apache.hc.core5.http.Header[] headers = c.baseRequest("/test", HttpMethod.POST, Map.of());
        Optional<org.apache.hc.core5.http.Header> op = Stream.of(headers).filter(e -> e.getName().equals("X-Lifecycle")).findFirst();
        Assertions.assertTrue(op.isPresent());
        Assertions.assertTrue(op.get().getValue().equals("Added"));
    }
}
