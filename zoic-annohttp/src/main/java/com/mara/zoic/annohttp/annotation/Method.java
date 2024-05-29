package com.mara.zoic.annohttp.annotation;

import com.mara.zoic.annohttp.http.HttpMethod;

import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求的表单变量（单个）。
 * <p>只能接受 {@link HttpMethod} 类型。且整个参数列表中如果有多个，则以第一个出现的为准。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Method {

}
