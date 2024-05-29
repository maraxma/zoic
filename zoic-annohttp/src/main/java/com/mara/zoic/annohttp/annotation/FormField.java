package com.mara.zoic.annohttp.annotation;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求的表单变量（单个）。
 * <p>只能接受 {@link String}、{@link File}、{@link InputStream}、byte[] 类型。
 * <p>如果所有表单变量全部都是String，那么将会使用 URLEncoded 方式发送，否则使用 Multipart 发送。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FormField {
    String value();
}
