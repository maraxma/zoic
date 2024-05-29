package com.mara.zoic.annohttp.annotation;

import com.mara.zoic.annohttp.http.proxy.RequestProxy;

import java.lang.annotation.*;

/**
 * 声明一个方法参数作为请求代理设置。该代理在每一次发起请求都会使用，但是仅仅影响本方法所代表的请求。
 * <p>该方法参数必须是 {@link RequestProxy} 类型。
 * <p>{@code @Proxy} 所修饰的参数是可以传入null的，传入null代表不使用代理。
 * <p>在 {@link Request#proxy()} 中同样可以设定代理，参见 {@link Request#proxy()} 获得更多信息。
 * <p>如果参数列表和 {@link Request#proxy()} 中一共出现超过一个代理设置（只能存在一个代理），那么会抛出异常。
 * @author MAara.X.Ma
 * @since 1.0.0 2022-07-22
 * @see RequestProxy
 * @see Request#proxy()
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Proxy {

}
