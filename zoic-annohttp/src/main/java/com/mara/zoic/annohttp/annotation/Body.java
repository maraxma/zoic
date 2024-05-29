package com.mara.zoic.annohttp.annotation;

import org.apache.hc.core5.http.ContentType;
import java.lang.annotation.*;

/**
 * 使用此注解为请求附加请求体。
 * <p>可以接受任何类型的数据，数据本身不能是null。
 * <p>如下是数据对照表：
 * <table>
 *     <tr><td>Java类型</td><td>默认Content-Type</td><td>可指定的Content-Type</td></tr>
 *     <tr><td>String</td><td>{@link ContentType#TEXT_PLAIN}</td><td>任意</td></tr>
 *     <tr><td>InputStream</td><td>{@link ContentType#APPLICATION_OCTET_STREAM}</td><td>无</td></tr>
 *     <tr><td>File</td><td>{@link ContentType#APPLICATION_OCTET_STREAM}</td><td>{@link ContentType#MULTIPART_FORM_DATA}</td></tr>
 *     <tr><td>byte[]</td><td>{@link ContentType#APPLICATION_OCTET_STREAM}</td><td>{@link ContentType#TEXT_PLAIN}
 *     <tr><td>Map&lt;String, Object&gt;</td><td>{@link ContentType#APPLICATION_JSON}</td><td>{@link ContentType#APPLICATION_XML}, {@link ContentType#TEXT_XML}, {@link ContentType#APPLICATION_FORM_URLENCODED}, {@link ContentType#MULTIPART_FORM_DATA}, application/yaml, application/yml, text/yaml, text/yml</td></tr>
 *     <tr><td>Other Java Objects</td><td>{@link ContentType#APPLICATION_JSON}</td><td>{@link ContentType#APPLICATION_XML}, {@link ContentType#TEXT_XML}, {@link ContentType#TEXT_PLAIN}, {@link ContentType#APPLICATION_OCTET_STREAM}, {@link ContentType#APPLICATION_OCTET_STREAM}, application/yaml, application/yml, text/yaml, text/yml</td></tr>
 * </table>
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-18
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Body {

    /**
     * 某些情况下可用于指定FormName。除开需要传送表单（Form）数据，一般情况下不需要指定。
     */
    String value() default "";
}
