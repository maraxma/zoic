# zoic-annohttp

annohttp 全称是 Annotation HTTP，是一个靠注解驱动的HTTP客户端，类似于retrofit，但基于HttpClient而非OKHttp，
并且提供了更多的更灵活的操作。

annohttp有如下的特性：
- 全注解驱动
- 支持自定义协议
- 支持针对单个请求的代理设定
- 支持自定义转换器
- 支持静态和动态baseUrl
- 支持异步请求
- 支持快速响应转换
- 支持SPEL

## 为什么使用annohttp

- 结构化项目，使你的代码易于阅读和管理
- 将项目中的HTTP请求变为Service，用服务的思想治理随处可见的HTTP请求
- 屏蔽HttpClient的复杂性，减少重复的请求代码

## 基本用法

```java
// ItemService.java
public interface ItemService {
    @Request(url = "http://yourhost:8080/item/get/{id}")
    ItemInfo getItemInfo(@PathVar("id") String id, @Headers Map<String, String> headers);
}

// Application.java
public class Application {
    // main
    public static void main(String[] args) {
        ItemService itemService = AnnoHttpClients.create(ItemService.class);
        ItemInfo itemInfo = itemService.getItemInfo("99", Map.of("JWT", "xxxxxx")); // 实际将请求http://yourhost:8080/item/get/99
    }
}
```

## 灵活请求

灵活请求是在基本用法的基础上，将返回值使用PreparingRequest包裹。此时，你在完成此方法调用后会得到PreparingRequest对象，该对象提供了很多灵活的请求方式，这当中也包含了对返回体的灵活处理。

注意：使用PreparingRequest包裹意味着请求操作会被滞后，真正发起请求需要调用PreparingRequest.request()方法。

```java
// ItemService.java
public interface ItemService {
    @Request(url = "http://yourhost:8080/item/get/{id}")
    PreparingRequest<ItemInfo> getItemInfo(@PathVar("id") String id, @Headers Map<String, String> headers);
}

// Application.java
public class Application {
    // main
    public static void main(String[] args) {
        ItemService itemService = AnnoHttpClients.create(ItemService.class);
        PreparingRequest<ItemInfo> itemInfoReq = itemService.getItemInfo("99", Map.of("JWT", "xxxxxx"));
        // 常规请求
        ItemInfo itemInfo = itemInfoReq.request();
        // 异步请求
        ItemInfo itemInfo2 = itemInfoReq.requestAsync(new MyExecutor());
        // 经典请求（返回HttpResponse对象，适合于需要自行处理响应的情况）
        HttpResponse response = itemInfoReq.requestClassically();
        // 在请求前修改请求头或请求参数(XXX代表具体的内容，参见源码)
        itemInfoReq.customXXX();
        // 将请求后的返回视为可操作性对象（可操作性对象提供了很多便捷方法，参见源码）
        OperableHttpResponse ohr = itemInfoReq.requestOperable();
        // 将响应转换为任意你想要的类型
        MyBean myBean = ohr.asJsonConvertible().toBean(MyBean.class);
        XX xx = ohr.asXXX.toXXX();
        // 更多有关OperableHttpResponse的方法参见源码或者IDE智能提示，OperableHttpResponse可以极大地方便对响应体的转换处理
    }
}
```

## 只关注输入流的请求

只关注输入流的请求只需要将返回类型改为InputStream即可。

```java
// ItemService.java
public interface ItemService {
    @Request(url = "http://yourhost:8080/item/get/{id}")
    InputStream getItemInfo(@PathVar("id") String id, @Headers Map<String, String> headers);
}
```

## 特别章节：OperableHttpResponse 对象

OperableHttpResponse对象由PreparingRequest.requestOperable()方法生成，该方法发起请求并在得到响应后生成一个OperableHttpResponse对象，OperableHttpResponse提供了对响应体的灵活处理。这些处理包括：

- 视响应体为字符串
- 视响应体为JSON
- 视响应体为XML
- 视响应体为YAML
- 视响应体为InputStream
- 视响应体为Java序列化后的序列
- 视响应体为字节数组

OperableHttpResponse是HttpResponse的子类，包含HttpResponse提供的所有方法，因此，既可以获得响应体也可以获得响应中的其他部分（如响应头、状态行等）。
但是asXX()方法只针对响应体。

推荐使用PreparingRequest.requestOperable()方法来获得OperableHttpResponse响应对象，这样你几乎可以做到任何事情。

OperableHttpResponse对象实现了Closable接口，因此需要注意在使用完成后手动关闭OperableHttpResponse对象。推荐使用try-with-resources来书写。

annohttp可以让用户通过方法的选择直接处理如上的情况，将其转换为具体的Java对象。

```
// 获得JSON反序列化后的JavaBean（XML、YAML同理）
MyBean bean = itemService.getItemInfo().requestOperable().asJsonConvertible().toBean(MyBean.class);
// 获得Java序列化后的反序列化对象
MyBean bean = itemService.getItemInfo().requestOperable().asJavaSerializedSequenceToObject(MyBean.class);
// 直接获得字符串
String respString = itemService.getItemInfo().requestOperable().asSequenceToString("UTF-8");
// 直接获得字节数组
byte[] bytes = itemService.getItemInfo().requestOperable().asSequenceToBytes();

// ...更多方法请自行探索
```

## 为单独的请求附加代理

很多情况下，我们只需要为单个请求添加代理，而目前主流的HTTP客户端框架都未曾提供此功能，它们的做法是一刀切，单个HTTP客户端所发起的请求都会被代理。annohttp则提供了一种更细粒度的代理设置方式，支持只针对某个特定的请求设定代理。

```java
import proxy.http.com.mara.annohttp.RequestProxy;

public interface ItemService {
    // 以注解方式添加代理（SPEL）
    @Request(url = "http://yourhost:8080/item/get/{id}", proxy = "T(com.mara.zoic.annohttp.RequestProxy).create('localhost', 8090, T(com.mara.zoic.annohttp.RequestProxy.ProxyType).HTTP, false, T(com.mara.zoic.annohttp" +
            "com.mara.zoic.annohttp.http.proxy.RequestProxy.ProxyCredentialType).NONE, null, null, null, null)")
    ItemInfo getItemInfo(@PathVar("id") String id, @Headers Map<String, String> headers);

    // 以参数方式添加代理
    @Request(url = "http://yourhost:8080/item/get/{id}")
    ItemInfo getItemInfoWithProxy(@PathVar("id") String id, @Headers Map<String, String> headers, @Proxy RequestProxy RequestProxy);
}
```

## 配合 spring 实现自动装配

1. 在SpringBoot项目的入口类上标注@EnableAnnoHttpAutoAssembling
2. 在一个或多个包下书写annohttp接口并将其标注@AnnoHttpService
3. 在你的spring配置文件中增加如下的项

```yaml
# application.yml
annohttp:
  connect-timeout-in-seconds: 10 
  socket-timeout-in-seconds: 10
  connection-idle-timeout-in-seconds: 20
  connection-timeout-in-seconds: 20
  max-connections: 80
  max-connections-per-route: 40
  keep-alive: false
  keep-alive-time-in-seconds: 30
  flow-redirect: true
  trust-any-ssl: true
  service-base-packages: [com.xx.xx.http.service, com.xx.xx.http.request]
```

4. 直接注入使用

```java
// 注入，然后直接使用
@Autowired
private ItemService itemService;
```

## 结合 SPEL 实现更灵活的操作

**此功能需需要 spring-expression 的支持，你需要引入依赖包**。

@Request注解上很多参数都支持SPEL表达式，它们的名称都以“Spel”结尾，如bodySpel、headerSpel等。请参见Request的javadoc获得更多信息。

所有的SPEL表达式上下文中都提供了如下的信息，可以直接在SPEL表达式中取用：

- #arg{id}: 对应请求方法上的参数列表，{id}需要替换为整数，从0开始，如#arg0代表第一个参数

Requst.successCondition接受一个字符串，该字符串必须是SPEL并返回boolean，以确定请求成功的条件。特殊的是，在这个SPEL的上下文中额外提供了：

- #httpResponse：此次请求的响应对象，可以通过其获得各种信息，如响应码、HTTP版本、响应头等等

```java
// 
public interface ItemService {
    // 以注解方式添加代理（SPEL）
    @Request(url = "http://yourhost:8080/item/get/{id}", successCondition = "#httpResponse.statusLine.statusCode==201",
    bodySpel = "Map.of(\"name\": \"mara\")")
    ItemInfo getItemInfo(@PathVar("id") String id, @Headers Map<String, String> headers);
}
```

## BaseURL支持

annohttp还支持BaseURL。BaseURL是基础URL，和@Request.url拼凑成完全的URL。这适用于请求在同一域名下各种子地址。

annohttp对于BaseURL有两种支持方式：

- 固定的BaseURL：提供一个固定的URL供使用

```java
// 硬编码方式
ItemService itemService = AnnoHttpClients.create(ItemService.class, "http://localhost:8080");
// Spring环境注解方式
// ItemService.java
@AnooHttpService(baseUrl = "http://localhost:8080")
public interface ItemService {}
```

- 动态的BaseURL，适合于动态获取BaseURL
```java
// 硬编码方式
ItemService itemService = AnnoHttpClients.create(ItemService.class, metadata -> metadata.getRequestAnnotation().method() == HttpMethod.GET ? "http://localhost:8081" : "http://localhost:9091");
// Spring环境注解方式
@AnnoHttpService(baseUrlFunctionClass = MyBaseUrlFunction.class)
public interface ItemService2 {}
```

由于@Request.url默认是空字符串，因此，配合动态BaseURL，你可以轻松地实现动态的URL，这非常适用于远程获取URL或者从数据库获取URL的场景。
```java
ItemService itemService = AnnoHttpClients.create(ItemService.class, metadata -> remoteService.getItemSerivceUrl());
```

这个特性有点类似于下文的“自定义协议"，只不过它们的实现方式不同，用途也稍微有点区别。

## 自定义协议

你可以使用自定义协议来定义你的URI，比如请求“myhttp://xxx”。当annohttp遇到这类协议的时候，
将会寻找合适的 ProtocolHandler 来处理。 本质上自定义的协议将会回归到HTTP请求中来。

自定义协议适用于需要配合其他中间件的需求，如你的url需要从远端获取，然后根据这个地址来请求数据，亦或是你们有特殊的业务场景，需要在请求前做一些特定的操作。有了自定义协议这项功能，你甚至可以改变你的url结构，使其变得更为优雅。

举例，新建一个协议，此协议名称是“iteminfo”，路径格式是“iteminfo://{serviceName}/{itemNumber}”。按照此协议具体的请求地址可以是“iteminfo://inventory/1001”，代表查询 ItemNumber 为 1001 的商品的库存。

下面的示例代码展示了如何定义一个新的自有协议。

```java
/********************************
 *
 * 如下的例子描述了一个自定义的协议“myhttp”
 * 该协议需要通过路径中的服务名称从远端查询获得真正的地址，然后通过此地址附加ItemNumber查询商品的价格
 * 这只是个例子，实际情况需要灵活运用
 * 通过自定义的ProtocolHandler，你几乎可以实现任何基于HTTP的其他和公司具体流程相关的请求动作
 *
 *******************************/


// MyHttpProtocolHandler.java
public class MyHttpProtocolHandler implements ProtocolHandler {
    @Override
    public String protocol() {
        return "myhttp";
    }

    @Override
    public void handle(AnnoHttpClientMetadata metadata, PreparingRequest<?> preparingRequest) {
        preparingRequest.customRequestUrl(oldUrl -> {
            String serviceName = oldUrl.replace("myhttp").split(":");
            String serviceUrl = serviceReg.getServiceUrl(serviceName);
            String itemNumber = (String) metadata.getRequestMethodArguments()[0];
            return new URIBuilder(serviceUrl).addParameter("itemNumber", itemNumber).toString();
        });
    }
}

// ItemService.java
public interface ItemService {

    @Request(url = "myhttp://service:item:{itemNumber}")
    BigDecimal getPrice(@PathVar("itemNumber") String itemNumber);
}

// UserCode
public class Main {
    public static void main(String[] args) {
        AnnoHttpClients.registerProtocolHandlers(new MyHttpProtocolHandler());
        ItemService itemService = AnnoHttpClients.create(ItemService.class);
        System.out.println(itemService.getPrice("SKU0001"));
    }
}
```

## 自定义转换器

annohttp提供了两类的转换器接口：

- RequestBodyConverter：请求体转换器，专门负责转换请求体（如将Map转换为urlencoded形式的请求体）
- ResponseConverter：响应转换器，专门负责将响应转换为用户需要的样子（如只提取响应头，只提取StatusLine，将响应体转换为Object等）

注意，是【请求体】转换器和【响应】转换器，对于发出的请求，只存在请求体转换器，对于接收的响应，则存在响应转换器，响应转换器里包含响应体转换器。

需要注意的是ResponseBodyConverter是ResponseConverter的子类，只负责响应体的转换。

annohttp内置的各种转换器足以应付大部分开发需求。

但是，我们可以自行实现上述的接口以扩展我们的处理范围或实现其他的需求。比如，针对一个响应头 Content-Type 为 “hex” 的响应，则提取其输入流，将其转换为hex文件。此时则需要自定义响应转换器。

**且，内置的转换器的优先级是低于用户自定义的转换器的。意即，当同时出现符合逻辑的转换器，将使用用户定义的转换器。**

转换器在编写完毕后，使用如下的方式告诉annohttp：

```java
AnnoHttpClients.registerRequestBodyConverter(MyRequestBodyConverter.class);
AnnoHttpClients.registerResponseConverter(MyResponseConverter.class);
```

## 自定义HTTP客户端

annohttp使用Apache HttpClient作为HTTP客户端，这点是无法更改的，且，该客户端在Spring环境下受配置文件的影响。但是如果你想要在请求中使用自定义的Apache HTTPClient，如你想要自定义某些参数、使用池化的HTTPClient等，则可以通过 PreparingRequest 的 customHttpClient 方法来完成：

```java

// ItemService.java
public interface ItemService {
    // 需要以 PreparingRequest 的形式返回，以支持自定义HTTPClient
    @Request(url = "http://yourhost:8080/item/get/{id}")
    PreparingRequest<ItemInfo> getItemInfo(@PathVar("id") String id, @Headers Map<String, String> headers);
}

// User code
ItemService itemService = AnnoHttpClients.create(ItemService.class);
PreparingRequest<ItemInfo> pr = itemService.getItemInfo("123", Map.of("name", "mara"));
itemService.customHttpClient(() -> new YourHttpClientBuilder()); // 接受一个Supplier，需要返回HttpClientBuilder对象
ItemInfo itemInfo = pr.request();
```

## 生命周期接口

生命周期接口 AnnoHttpLifecycle 提供一些钩子函数供使用。钩子函数大部分都是只读的，除了 beforeClientRequesting 方法，其可以用来在请求发起之前做一些更改。其他的钩子函数可以用来做一些触发机制或者是日志记录。

AnnoHttpLifecycle 接口实例可以添加多个。要添加一个声明周期接口实例，通请使用AnnoHttpClients.addAnnoHttpLifecycleInstances() 方法。

AnnoHttpLifecycle 接口实例被添加后是全局生效的。如果只是想针对某个客户端应用钩子函数，可以使用钩子函数签名上提供的 HttpClientMetadata 参数来判定。

AnnoHttpLifecycle 接口实例按照被添加的顺序依次被调用。

如下代码介绍了 AnnoHttpLifecycle 的使用方式：

```java
interface Client {
            @Request(url = "/testXXX", successCondition = "true")
            org.apache.hc.core5.http.Header[] baseRequest(@Url String url, @Method HttpMethod method, @Body Map<String, Object> body);
}
class LifecycleDemo implements AnnoHttpLifecycle {

    @Override
    public void beforeClientCreating(Class<?> clientClass) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void afterClientCreated(Object client) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void beforeClientRequesting(HttpClientMetadata httpClientMetadata, PreparingRequest<?> preparingRequest) {
        if (httpClientMetadata.getServiceClientClass() == Client.class) {
            preparingRequest.customRequestHeaders(t -> t.add(new CoverableNameValuePair("X-Lifecycle", "Added")));
        }
    }

    @Override
    public void afterClientRequested(HttpClientMetadata httpClientMetadata, HttpResponse httpResponse,
            ResponseConverter responseConverter) {
        // TODO Auto-generated method stub
        
    }
}
// 生成客户端
Client c = AnnoHttpClients.create(Client.class, "http://localhost:8081/");
// 注册声明周期实例
AnnoHttpClients.addAnnoHttpLifecycleInstances(new LifecycleDemo());
// 开始请求
c.XXX();
```

在 Spring 环境中，你只需将你的 AnnoHttpLifecycle 实例注册到 Spring 容器即可完成自动应用。

```java
@Bean
public static AnnoHttpLifecycle annoHttpLifecycle() {
    return new MyLifecycle();
}
```

另一种在 Spring 环境中注册的方式是在使用Spring的声明周期接口如 ApplicationRunner 在合适的时机使用 AnnoHttpClients.addAnnoHttpLifecycleInstances() 方法来注册。

## 请求方法返回参数一览表

该表描述了annohttp目前所支持的请求方法返回参数类型。

参数类型既可以直接书写在接口方法的返回类型，又可以作为PreparingRequest的参数化类型。

注意，除开一些特殊的类型，大部分类型都是指代响应体的，它用于告诉annohttp使用者期望将响应体转换为什么类型。

示例：

```java
public interface ItemService {
    // 以注解方式添加代理（SPEL）
    @Request(url = "http://yourhost:8080/item/get/{id}")
    _返回类型_ getItemInfo(@PathVar("id") String id, @Headers Map<String, String> headers);

    @Request(url = "http://yourhost:8080/item/get/{id}")
    PreparingRequest<_返回类型_> getItemInfo(@PathVar("id") String id, @Headers Map<String, String> headers);
}
```

| 类型 | 说明 |
|-----|-----|
|任意Bean|将响应体转换为这个指定的JavaBean，需要配合响应头中的Content-Type做转换，比如"application/json"，此时annohttp会将响应体看做JSON字符串并将其按照JSON转换为JavaBean，请注意响应头中的Content-Type值的书写规范。同理annohttp也支持xml和yaml的转换。还有一种特殊情况，当Content-Type的值为"application/octet-stream"时，annohttp会尝试将响应体直接以Java序列化的方式将流转换为JavaBean|
|void|只关心请求，不关心任何响应|
|Void|同上，返回时会被置为唯一值`null`|
|StatsLine|只关心StatusLine（响应行）|
|Header[]|只关心响应头|
|InputStream|只关心响应体，以流形式返回，请注意自行关闭|
|byte[]|只关心响应体，以字节数组形式返回|
|ClassicHttpResponse|只关心响应体，以ClassicHttpResponse形式返回（这是HttpClient5的经典响应类型，可以自由从中获取到想要的信息，注意使用完毕关闭流）|


## 注解一览表

可供使用的注解全部位于com.mara.zoic.annohttp.annotation包下。

| 名称               | 目标                          | 作用                           |
|------------------|-----------------------------| ------------------------------ |
| @Request         | 标注于接口的抽象方法上                 | 声明这是一个HTTP请求方法       |
| @Body            | 标注于@Request修饰的方法的参数上        | 声明此参数是该HTTP请求的请求体 |
| @AnnoHttpService | 标注于接口类型上 | 向Spring说明是一个HTTP服务 |
| ... 不想写了，请参见源码   |                             |                                |


COPYRIGHT @ Mara.X.Ma 2022