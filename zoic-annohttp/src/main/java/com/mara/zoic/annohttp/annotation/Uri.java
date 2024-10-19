package com.mara.zoic.annohttp.annotation;

import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求的URI。
 * <p>能接受String和URI。且整个参数列表中如果有多个，则以第一个出现的为准。URI类型的则无需再标注此注解，String类型的要作为URI则必须标注此注解。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Uri {

}
