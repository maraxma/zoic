package com.mara.zoic.annohttp;


import com.mara.zoic.annohttp.annotation.*;
import com.mara.zoic.annohttp.http.AnnoHttpClients;
import com.mara.zoic.annohttp.http.HttpMethod;
import com.mara.zoic.annohttp.http.PreparingRequest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.StatusLine;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
            @Request(url = "http://localhost:8081/test")
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
    @DisplayName("普通测试 -- 默认GET方式，自定义ContentType")
    void baseTest2() {
        interface Client {
            @Request(url = "http://localhost:8081/test")
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
            @Request(url = "http://localhost:8081/test")
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
            @Request(url = "http://localhost:8081/test")
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
            @Request(url = "http://localhost:8081/test")
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
            @Request(url = "http://localhost:8081/test", headers = {"Mara: 1", "Mara: 2"})
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
            @Request(url = "http://localhost:8081/test", headers = {"Mara: 1", "Mara: 2"})
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
            @Request(url = "http://localhost:8081/test", successCondition = "true")
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
            @Request(url = "http://localhost:8081/test", successCondition = "")
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
    @DisplayName("普通测试 -- 默认GET方式，附加baseUrl")
    void baseTest10() {

        interface Client {
            @Request(url = "/test", successCondition = "true")
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
            @Request(url = "/test", successCondition = "true")
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
            @Request(url = "/test", successCondition = "true")
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
            @Request(url = "/test", successCondition = "true")
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
    @DisplayName("普通测试 -- 默认GET方式，通过@FormFields发送表单(使用Map)")
    void baseTest14() {

        interface Client {
            @Request(url = "/test", successCondition = "true")
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
            @Request(url = "/test", successCondition = "true")
            String baseRequest(@FormField("Name") String name, @FormField("Age") String age);

            @Request(url = "/test", successCondition = "true", contentType = "application/x-www-form-urlencoded")
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
            @Request(url = "/test", successCondition = "true")
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
            @Request(url = "/test", successCondition = "true")
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
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Url指定Url")
    void baseTest18() {

        interface Client {
            @Request(url = "/testXXX", successCondition = "true")
            StatusLine baseRequest(@Url String url);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        StatusLine statusLine = c.baseRequest("/test");

        Assertions.assertEquals(200, statusLine.getStatusCode());
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用@Method指定method")
    void baseTest19() {

        interface Client {
            @Request(url = "/testXXX", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Url String url, @Method HttpMethod method);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        org.apache.hc.core5.http.Header[] headers = c.baseRequest("/test", HttpMethod.POST);

        Assertions.assertEquals("POST", Arrays.stream(headers).filter(e -> "Request-Method".equals(e.getName())).findFirst().map(org.apache.hc.core5.http.Header::getValue).orElse(null));
    }

    @Test
    @DisplayName("普通测试 -- 默认GET方式，在参数列表中使用多个@Body，预期抛出异常")
    void baseTest20() {

        interface Client {
            @Request(url = "/testXXX", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Url String url, @Method HttpMethod method, @Body Map<String, Object> body1, @Body Map<String, Object> body2);
        }

        Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
        Executable executable =  () -> c.baseRequest("/test", HttpMethod.POST, Map.of(), Map.of());

        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertTrue(e.getMessage() != null && e.getMessage().contains("You cannot use more than 1 @Body"));
    }

}
