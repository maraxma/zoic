package com.mara.zoic.annohttp.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.util.concurrent.ConcurrentHashMap;

public class JacksonComponentHolder {

    private static final ConcurrentHashMap<String, JsonMapper> OBJECT_MAPPER_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, XmlMapper> XML_MAPPER_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, YAMLMapper> YAML_MAPPER_CACHE = new ConcurrentHashMap<>();

    private static final JacksonCapitalizePropertyNamingStrategy NAMING_STRATEGY = new JacksonCapitalizePropertyNamingStrategy();

    /**
     * 获得ObjectMapper的单例。有如下参数的4个设置，设置相同的将获得相同的ObjectMapper。
     *
     * @param ignoreCase          忽略大小写（反序列化）
     * @param ignoreUnknownField  允许未知的字段出现（反序列化）
     * @param ignoreNullProperty  忽略值为NULL的字段（序列化）
     * @param capitalizeFieldName 字段首字母大写 （序列化）
     * @return {@link JsonMapper} 实例
     */
    public static JsonMapper getJsonMapper(boolean ignoreCase, boolean ignoreUnknownField, boolean ignoreNullProperty, boolean capitalizeFieldName) {
        String key = ignoreCase + "|" + ignoreUnknownField + "|" + ignoreNullProperty + "|" + capitalizeFieldName;
        return OBJECT_MAPPER_CACHE.compute(key, (k, v) -> {
            try {
                if (v == null) {
                    return buildAndSetObjectMapper(JsonMapper.builder(), ignoreCase, ignoreUnknownField, ignoreNullProperty, capitalizeFieldName);
                }
                return v;
            } catch (Exception e) {
                throw new RuntimeException("Cannot get or create JsonMapper for key: " + key, e);
            }
        });
    }

    /**
     * 获得XmlMapper的单例。有如下参数的4个设置，设置相同的将获得相同的XmlMapper。
     *
     * @param ignoreCase          忽略大小写（反序列化）
     * @param ignoreUnknownField  允许未知的字段出现（反序列化）
     * @param ignoreNullProperty  忽略值为NULL的字段（序列化）
     * @param capitalizeFieldName 字段首字母大写 （序列化）
     * @return {@link XmlMapper} 实例
     */
    public static XmlMapper getXmlMapper(boolean ignoreCase, boolean ignoreUnknownField, boolean ignoreNullProperty, boolean capitalizeFieldName) {
        String key = ignoreCase + "|" + ignoreUnknownField + "|" + ignoreNullProperty + "|" + capitalizeFieldName;
        return XML_MAPPER_CACHE.compute(key, (k, v) -> {
            try {
                if (v == null) {
                    return buildAndSetObjectMapper(XmlMapper.builder(), ignoreCase, ignoreUnknownField, ignoreNullProperty, capitalizeFieldName);
                }
                return v;
            } catch (Exception e) {
                throw new RuntimeException("Cannot get or create XmlMapper for key: " + key, e);
            }
        });
    }

    /**
     * 获得YAMLMapper的单例。有如下参数的4个设置，设置相同的将获得相同的YAMLMapper。
     *
     * @param ignoreCase          忽略大小写（反序列化）
     * @param ignoreUnknownField  允许未知的字段出现（反序列化）
     * @param ignoreNullProperty  忽略值为NULL的字段（序列化）
     * @param capitalizeFieldName 字段首字母大写 （序列化）
     * @return {@link YAMLMapper} 实例
     */
    public static YAMLMapper getYamlMapper(boolean ignoreCase, boolean ignoreUnknownField, boolean ignoreNullProperty, boolean capitalizeFieldName) {
        String key = ignoreCase + "|" + ignoreUnknownField + "|" + ignoreNullProperty + "|" + capitalizeFieldName;
        return YAML_MAPPER_CACHE.compute(key, (k, v) -> {
            try {
                if (v == null) {
                    return buildAndSetObjectMapper(YAMLMapper.builder(), ignoreCase, ignoreUnknownField, ignoreNullProperty, capitalizeFieldName);
                }
                return v;
            } catch (Exception e) {
                throw new RuntimeException("Cannot get or create YAMLMapper for key: " + key, e);
            }
        });
    }

    /**
     * 新建ObjectMapper并且设置它。
     *
     * @param mapperBuilder       建造者
     * @param ignoreCase          忽略大小写（反序列化）
     * @param ignoreUnknownField  允许未知的字段出现（反序列化）
     * @param ignoreNullProperty  忽略值为NULL的字段（序列化）
     * @param capitalizeFieldName 字段首字母大写 （序列化）
     */
    private static <T extends ObjectMapper> T buildAndSetObjectMapper(MapperBuilder<T, ?> mapperBuilder, boolean ignoreCase, boolean ignoreUnknownField, boolean ignoreNullProperty, boolean capitalizeFieldName) {
        if (ignoreCase) {
            mapperBuilder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        } else {
            mapperBuilder.disable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        }

        if (ignoreUnknownField) {
            mapperBuilder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        } else {
            mapperBuilder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }

        if (ignoreNullProperty) {
            mapperBuilder.defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
        } else {
            mapperBuilder.defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.USE_DEFAULTS, JsonInclude.Include.USE_DEFAULTS));
        }

        if (capitalizeFieldName) {
            mapperBuilder.propertyNamingStrategy(NAMING_STRATEGY);
        } else {
            mapperBuilder.propertyNamingStrategy(null);
        }

        return mapperBuilder.build();
    }
}
