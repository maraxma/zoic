package com.mara.zoic.annohttp.annotation;

import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求的查询参数。
 * <p>只能接受如下的类型：</p>
 * <ul>
 *     <li>Map&lt;String, String&gt;：每个 Entry 指定查询键名称和其值。</li>
 * </ul>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Queries {
}
