package com.mara.zoic.annohttp.http.request.converter;

import com.mara.zoic.annohttp.annotation.Request;
import com.mara.zoic.annohttp.http.Converter;
import com.mara.zoic.annohttp.http.HttpClientMetadata;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

/**
 * 请求体转换器。负责将请求体转换为合适的 {@link HttpEntity} 实例。
 * <p>适用于 {@link Request#requestBodyConverter()} ，如果直接在上面指定除 {@link AutoRequestBodyConverter} 以外的其他 {@link RequestBodyConverter}，
 * 那么将不会调用 {@link #canConvert(Object, ContentType, HttpClientMetadata, String)}  而是直接开始转换。
 *
 * @author Mara.X.Ma
 * @see Request#requestBodyConverter()
 * @since 1.0.0 2022-07-19
 */
public interface RequestBodyConverter extends Converter {

    /**
     * 将请求体转换为 {@link HttpEntity} 实例。
     *
     * @param source                 请求体实例，不会是null
     * @param contentType            用户指定的ContentType，可能是null
     * @param annoHttpClientMetadata 附加的元数据，不会是null
     * @param formFieldName          如果以表单形式转换，该body在表单中的名称，可能是null
     * @return {@link HttpEntity} 实例
     */
    HttpEntity convert(Object source, ContentType contentType, HttpClientMetadata annoHttpClientMetadata, String formFieldName);

    /**
     * 判定此转换器是否适用。
     *
     * @param source                 请求体实例，不会是null
     * @param contentType            用户指定的ContentType，可能是null
     * @param annoHttpClientMetadata 附加的元数据，不会是null
     * @param formFieldName          如果以表单形式转换，该body在表单中的名称
     * @return 如果适用则返回true，否则返回false
     */
    boolean canConvert(Object source, ContentType contentType, HttpClientMetadata annoHttpClientMetadata, String formFieldName);
}
