package com.mara.zoic.annohttp.annotation;

import java.lang.annotation.*;

/**
 * 快速请求头设定。附加 Content-Type: *&#47;*; charset=ISO-8859-1（{@link org.apache.hc.core5.http.ContentType#WILDCARD}） 请求头。
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2022-07-08
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ContentTypeWildcard {

}
