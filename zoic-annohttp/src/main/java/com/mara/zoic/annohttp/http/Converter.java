package com.mara.zoic.annohttp.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 代表一个消息体转换器。消息体转换器主要用于转换请求消息体和响应消息体。
 */
public interface Converter {
    Charset DEFAULT_STRING_CHARSET = StandardCharsets.UTF_8;
    String DEFAULT_STRING_FORM_FIELD_NAME = "String";
    String DEFAULT_FILE_FORM_FIELD_NAME = "File";
    String CONTENT_TYPE_APPLICATION_YAML = "application/yaml";
    String CONTENT_TYPE_APPLICATION_YML = "application/yml";
    String CONTENT_TYPE_TEXT_YAML = "text/yaml";
    String CONTENT_TYPE_TEXT_YML = "text/yml";
}
