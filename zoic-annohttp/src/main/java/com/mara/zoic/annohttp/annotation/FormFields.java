package com.mara.zoic.annohttp.annotation;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求的表单变量。
 * <p>只能接受如下的类型：</p>
 * <ul>
 *     <li>Map&lt;String, Object&gt;：每个 Entry 指定表单域名称和其值。Map的Value是一个Object，可以指定为 {@link File}、{@link InputStream}、byte[] 类型。</li>
 * </ul>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FormFields {

}
