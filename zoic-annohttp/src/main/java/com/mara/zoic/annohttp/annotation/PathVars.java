package com.mara.zoic.annohttp.annotation;

import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求的路径变量。
 * <p>只能接受如下的类型：</p>
 * <ul>
 *     <li>Map&lt;String, String&gt;：每个 Entry 指定变量名称和其值。</li>
 * </ul>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PathVars {
}
