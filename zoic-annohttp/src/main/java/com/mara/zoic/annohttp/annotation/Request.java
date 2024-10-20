package com.mara.zoic.annohttp.annotation;

import com.mara.zoic.annohttp.http.AnnoHttpClients;
import com.mara.zoic.annohttp.http.HttpMethod;
import com.mara.zoic.annohttp.http.PreparingRequest;
import com.mara.zoic.annohttp.http.proxy.RequestProxy;
import com.mara.zoic.annohttp.http.request.converter.AutoRequestBodyConverter;
import com.mara.zoic.annohttp.http.request.converter.RequestBodyConverter;
import com.mara.zoic.annohttp.http.response.converter.AutoResponseConverter;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;
import com.mara.zoic.annohttp.http.visitor.BaseResponseVisitor;
import com.mara.zoic.annohttp.http.visitor.ResponseVisitor;

import java.lang.annotation.*;
import java.util.concurrent.Executor;

/**
 * 标识一个方法作为HTTP的请求方法。
 * <p>此注解只能应用在接口的抽象方法上。</p>
 * <p>一个接口如果需要用作HTTP客户端，那么其下的所有的抽象方法都应当附加注解@Request，然后使用 {@link AnnoHttpClients#create(Class)} 方法将此接口实例化。</p>
 * <p>被@Request标注的抽象方法在被调用时会实际发起HTTP请求，然后直接返回期望的结果。这涉及到该方法的返回值的设定。目前annohttp支持几乎所有的类型作为返回值，annohttp会尝试为用户转换。不过如下几种特殊的返回值有其特殊意义需要单独说明。</p>
 * <p>
 *     <table>
 *         <tr><td>返回类型<td/><td>说明<td/></tr>
 *         <tr><td>{@link org.apache.hc.core5.http.Header}[]<td/><td>直接返回该次请求的所有响应头。用户可以通过它处理响应头。当然，响应头之外的内容直接被丢弃。<td/></tr>
 *         <tr><td>{@link org.apache.hc.core5.http.message.StatusLine}<td/><td>直接返回该次请求的状态行对象。用户可以通过它获得响应状态。当然，状态行之外的内容直接被丢弃。<td/></tr>
 *         <tr><td>{@link org.apache.hc.core5.http.ClassicHttpResponse}<td/><td>直接返回apache httpclient原生的 {@link org.apache.hc.core5.http.ClassicHttpResponse} 对象。通过该对象几乎可以达到最大的灵活处理。<td/></tr>
 *         <tr><td>{@link java.io.InputStream}<td/><td>直接返回响应体 {@link java.io.InputStream} 输入流。用户可以通过它处理响应体。<td/></tr>
 *         <tr><td>PreparingRequest&lt;T&gt;<td/><td>返回annohttp提供的 {@link PreparingRequest} 实例。用户可以通过它发起异步请求以及获得可操作的响应体（{@link com.mara.zoic.annohttp.http.OperableHttpResponse}）。
 *         特别地，如果以此类型作为返回类型，在实际调用此方法的时候并不会直接发起请求，而是返回一个准备中的预请求对象PreparingRequest，只有当PreparingRequest中的request相关的方法被调用时才会真正发起请求。
 *         另外。如果期望以异步的方式发起请求，要么自己在子线程中处理，要么使用 {@link PreparingRequest#requestAsync(Executor)} 。<td/></tr>
 *     </table>
 * </p>
 * <p>此注解中的很多参数都支持SpEl。引入SpEl的目的是使“常量字符串”变得“可计算”，增加灵活性。</p>
 *
 * <p>SpEl 内部提供的可使用变量如下：</p>
 * <ul>
 *     <li>#argN：参数列表中的第N个参数，N从0开始。</li>
 *     <li>#headers：已经计算好的请求头，是一个 List 对象，里面包含name和value</li>
 *     <li>#queries：已经计算好的查询参数，是一个List 对象，里面包含name和value</li>
 * </ul>
 *
 * @author Mara.X.Ma
 * @since 1.0.0
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Request {

    /**
     * 指定目标地址。
     */
    String uri() default "";

    /**
     * 指定目标地址，使用 SpEl 。 SpEl 必须返回一个字符串。
     */
    String uriSpel() default "";

    /**
     * 指定请求方法。
     */
    HttpMethod method() default HttpMethod.GET;

    /**
     * 指定请求的消息内容类型。
     * <p>虽然它应该在 #headers 中设定，但是为了方便，这里也可以。</p>
     * <p>Content-Type的设定可以同时设定charset。比如 {@code ContentType: Application/json; charset=UTF-8}。</p>
     */
    String contentType() default "";

    /**
     * 指定请求头。
     * <p>根据RFC文档，请求头是允许存在重复（同名）的。因此，这里的请求头同样可以重复，在这里指定的重复请求头在最终发送请求时相互之间不会被覆盖。</p>
     * <p>请求头应该是 XXX: XXX 形式。如 Content-Type: application/json 。</p>
     */
    String[] headers() default {};

    /**
     * 使用 SpEl 指定请求头。
     * <p>此Spel表达式应该返回一个List或者Map。</p>
     * <ul>
     *     <li>
     *         返回 Map ：返回 Map 适用于快速开发，功能简单。
     *         <pre>
     *              {
     *                  "Content-Type": "application/json"
     *              }
     *         </pre>
     *     </li>
     *     <li>
     *         返回 List ：返回 List 的好处是可以指定重复的请求头并且可以设定单个请求头是否被可以方法参数列表中的请求头覆盖。
     *         要特别注意在SpEl中的数组的写法：
     *         <pre>new Object[] {
     *              {
     *                  {
     *                      "name": "Content-Type",
     *                      "value": "application/json"
     *                      "coverable": false // 当设定为false时此请求头会和其他请求头共存，即使名称一样，即使 {@link #headerCoverable()} 设定为true。
     *                  }
     *              }
     *         </pre>
     *     </li>
     * </ul>
     * <p>有关两种 headers 的设定方法只能使用其中一种，否则程序会报错并提醒改正。</p>
     */
    String headersSpel() default "";

    /**
     * 如果存在同名的请求头，是否允许覆盖。
     * <p>默认是 false ，即不允许覆盖。</p>
     * <p>若允许覆盖，在目标方法参数中指定的请求头的优先级要高于此处指定的。</p>
     */
    boolean headerCoverable() default false;

    /**
     * 指定查询参数。
     * <p>根据 RFC 文档，查询参数是允许存在重复（同名）的。因此，这里的查询参数同样可以重复，在这里指定的重复查询参数在最终发送请求时相互之间不会被覆盖。</p>
     * <p>查询参数应该是 XXX=XXX 形式。如 id=1 。</p>
     */
    String[] queries() default {};

    /**
     * 使用 SpEl 指定查询参数。
     * <p>此Spel表达式应该返回一个 List 或者 Map 。</p>
     * <ul>
     *     <li>
     *         返回 Map ：返回 Map 适用于快速开发，功能简单。
     *         <pre>
     *              {
     *                  "Content-Type": "application/json"
     *              }
     *         </pre>
     *     </li>
     *     <li>
     *         返回List：返回 List 的好处是可以指定重复的查询参数并且可以设定单个参数是否可以被方法参数列表中的查询参数覆盖。
     *         <pre>要特别注意在SpEl中的数组的写法：
     *         new Object[]  {
     *                  {
     *                      "name": "id",
     *                      "value": "1"
     *                      "coverable": false // 当设定为 false 时此请求头会和其他请求头共存，即使名称一样，即使 {@link #queryCoverable()} 设定为 true 。
     *                  }
     *              }
     *         </pre>
     *     </li>
     * </ul>
     * <p>有关两种 queries 的设定方法只能使用其中一种，否则程序会报错并提醒改正。</p>
     */
    String queriesSpel() default "";

    /**
     * 如果存在同名的查询参数，是否允许覆盖。
     * <p>默认是 false ，即不允许覆盖。</p>
     * <p>若允许覆盖，在目标方法参数中指定的请求头的优先级要高于此处指定的。</p>
     */
    boolean queryCoverable() default false;

    /**
     * 字符串形式的请求体。
     * <p>注意：有关请求体，一般来说 GET 方法和一些特有的方法不应具有请求体，但是 RFC 文档并未禁止，因此，在 annohttp 中，我们允许在 GET 和一些其他请求上附加 Body 。请根据实际情况使用。</p>
     * <p>注意：如果存在多个请求体设定，那么程序将会抛出异常提醒修正。</p>
     */
    String bodyString() default "";

    /**
     * 附加字节数组形式的请求体。
     * <p>注意：有关请求体，一般来说 GET 方法和一些特有的方法不应具有请求体，但是 RFC 文档并未禁止，因此，在 annohttp 中，我们允许在 GET 和一些其他请求上附加 Body 。请根据实际情况使用。</p>
     * <p>注意：如果存在多个请求体设定，那么程序将会抛出异常提醒修正。</p>
     */
    byte[] bodyBytes() default {};

    /**
     * 使用 SpEl 设定请求体。
     * <p>SpEl 应当返回一个字符串或者对象。annohttp 会根据具体的Content-Type采用不同的转换器。<p/>
     */
    String bodySpel() default "";

    /**
     * 设置代理，必须符合SpEl表达式，必须返回 {@link RequestProxy} 对象。
     */
    String proxy() default "";

    /**
     * 请求获得连接的超时时间。单位秒。默认为180。
     */
    int connectionRequestTimeoutInSeconds() default 180;

    /**
     * 等待响应的超时时间。单位秒。默认为60。
     */
    int responseTimeoout() default 60;

    /**
     * 是否关闭跟随重定向。默认为false。
     */
    boolean disableRedirects() default false;

    /**
     * 如何判断访问成功。必须是SpEl表达式且必须返回boolean。如果是空字符串，则代表不附加任何条件。
     * <p>默认值为“#status==200”，代表当响应码为200时认为访问成功。这个时候才开始做响应体的自动转换。
     * <p>当不涉及到响应体的自动转换时此参数无效。
     * <p>SpEl表达式中你将可以获得如下的额外变量（基础变量仍然提供，参见本类上的文档）：
     * <ul>
     * <li>httpResponse: 本次请求的原始响应
     * </ul>
     */
    String successCondition() default "#httpResponse.code==200";

    /**
     * 设定响应的字符编码。当需要将响应流转换为String时需要此参数。
     * <p>默认是"UTF-8"。</p>
     * <p>此参数受 {@link #preferUsingResponseCharset()} 影响。</p>
     *
     * @see #preferUsingResponseCharset()
     */
    String responseCharset() default "UTF-8";

    /**
     * 是否优先使用响应中给出的字符编码。
     * <p>设定为true时，将优先使用响应中附带的字符编码，如果响应中不存在才使用 {@link #responseCharset()} 设定的值。
     * <p>默认是true。当需要强制使用指定的字符编码的时候请将此参数设定为false，并且设定好 {@link #responseCharset()} 的值。</p>
     *
     * @see #responseCharset()
     */
    boolean preferUsingResponseCharset() default true;

    /**
     * 指定请求体转换器。
     * <p>默认是内置的 {@link AutoRequestBodyConverter}。</p>
     *
     * @see RequestBodyConverter
     */
    Class<? extends RequestBodyConverter> requestBodyConverter() default AutoRequestBodyConverter.class;

    /**
     * 指定响应转换器。
     * <p>默认是内置的 {@link AutoResponseConverter}。</p>
     *
     * @see ResponseConverter
     */
    Class<? extends ResponseConverter> responseConverter() default AutoResponseConverter.class;

    /**
     * 设定响应前置访问器。该前置访问器先于任何 {@link AbstractResponseBodyConverter}，同样也优先于 {@link #successCondition()} 的执行。
     */
    Class<? extends ResponseVisitor> responseVisitor() default BaseResponseVisitor.class;

    /**
     * 指定响应体的内容类型。
     * <p>此设定受 {@link #preferUsingResponseContentType()} 影响。</p>
     *
     * @see #preferUsingResponseContentType()
     */
    String responseContentType() default "application/json";

    /**
     * 是否优先使用响应中的ContentType。
     * <p>默认是true。当为true时，优先使用响应头中的ContentType，{@link #responseContentType()} 不生效。如果为true，但是响应中不存在ContentType，
     * 那么 {@link #responseContentType()} 将会被使用。</p>
     */
    boolean preferUsingResponseContentType() default true;
}
