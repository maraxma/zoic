package com.mara.zoic.annohttp.annotation;

import com.mara.zoic.annohttp.http.AnnoHttpClients;
import com.mara.zoic.annohttp.http.HttpClientMetadata;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * 声明一个接口作为annohttp服务接口（HTTP客户端）。
 * <p>使用spring自动装配时必须在接口上附加此注解；当自行使用 {@link AnnoHttpClients#create(Class)} 创建服务时，接口上可以不标注此注解。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AnnoHttpService {
    String baseUri() default "";

    Class <? extends Function<HttpClientMetadata, String>> baseUriFunctionClass() default EmptyBaseUriFunction.class;

    class EmptyBaseUriFunction implements Function<HttpClientMetadata, String> {

        @Override
        public String apply(HttpClientMetadata annoHttpClientMetadata) {
            return "";
        }
    }
}
