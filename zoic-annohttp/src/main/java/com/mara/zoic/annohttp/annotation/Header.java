package com.mara.zoic.annohttp.annotation;

import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求的请求头（单个）。
 * <p>只能接受String。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Header {
    String value();
}
