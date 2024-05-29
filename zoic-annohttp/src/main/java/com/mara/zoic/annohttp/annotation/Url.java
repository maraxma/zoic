package com.mara.zoic.annohttp.annotation;

import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求的URL。
 * <p>只能接受String。且整个参数列表中如果有多个，则以第一个出现的为准。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Url {

}
