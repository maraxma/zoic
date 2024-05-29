package com.mara.zoic.annohttp.http;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mara.zoic.annohttp.http.exception.ConversionException;
import com.mara.zoic.annohttp.http.serialization.TypeRef;
import org.apache.hc.core5.http.ClassicHttpResponse;

import java.util.*;

public abstract class AbstractJacksonConvertible extends AbstractStringBasedConvertible {

    protected AbstractJacksonConvertible(ClassicHttpResponse httpResponse, String charset) {
        super(httpResponse, charset);
    }

    protected ObjectMapper objectMapper;

    /**
     * 直接返回该对象的JSON字符串形式。
     * @return JSON字符串
     */
    @Override
    public String toString() {
        return objectString;
    }

    @Override
    public <T> T toBean(Class<T> clazz) {
        Objects.requireNonNull(clazz);
        try {
            return objectString == null ? null : objectMapper.readValue(objectString, clazz);
        } catch (JsonProcessingException e) {
            throw new ConversionException("Cannot convert response string '" + objectString + "' to class '" + clazz + "'", e);
        }
    }

    @Override
    public <K, V> Map<K, V> toMap(Class<K> keyClass, Class<V> valueClass) {
        return toMap(HashMap.class, keyClass, valueClass);
    }

    @Override
    public <K, V> Map<K, V> toMap(@SuppressWarnings("rawtypes") Class<? extends Map> mapClass, Class<K> keyClass, Class<V> valueClass) {
        Objects.requireNonNull(mapClass);
        Objects.requireNonNull(keyClass);
        Objects.requireNonNull(valueClass);
        Objects.requireNonNull(keyClass);
        Objects.requireNonNull(valueClass);
        try {
            return objectString == null ? null : objectMapper.readValue(objectString, objectMapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass));
        } catch (JsonProcessingException e) {
            throw new ConversionException("Cannot convert response string '" + objectString + "' to Map", e);
        }
    }

    @Override
    public <T> List<T> toList(Class<T> elementClass) {
        return toList(ArrayList.class, elementClass);
    }

    @Override
    public <T> List<T> toList(@SuppressWarnings("rawtypes") Class<? extends List> listClass, Class<T> elementClass) {
        Objects.requireNonNull(listClass);
        Objects.requireNonNull(elementClass);
        try {
            return objectString == null ? null : objectMapper.readValue(objectString, objectMapper.getTypeFactory().constructCollectionType(listClass, elementClass));
        } catch (JsonProcessingException e) {
            throw new ConversionException("Cannot convert response string '" + objectString + "' to List", e);
        }
    }

    @Override
    public <K, V> List<Map<K, V>> toListMap(Class<K> keyClass, Class<V> valueClass) {
        return toListMap(List.class, Map.class, keyClass, valueClass);
    }

    @Override
    public <K, V> List<Map<K, V>> toListMap(@SuppressWarnings("rawtypes") Class<? extends List> listClass,
                                            @SuppressWarnings("rawtypes") Class<? extends Map> mapClass, Class<K> keyClass, Class<V> valueClass) {
        Objects.requireNonNull(listClass);
        Objects.requireNonNull(mapClass);
        Objects.requireNonNull(keyClass);
        Objects.requireNonNull(valueClass);
        try {
            return objectString == null ? null : objectMapper.readValue(objectString, objectMapper.getTypeFactory()
                    .constructCollectionType(listClass, objectMapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass)));
        } catch (JsonProcessingException e) {
            throw new ConversionException("Cannot convert response string '" + objectString + "' to ListMap", e);
        }
    }

    @Override
    public <T> T toCollection(@SuppressWarnings("rawtypes") Class<? extends Collection> collectionClass, Class<?> elementClass) {
        Objects.requireNonNull(collectionClass);
        Objects.requireNonNull(elementClass);
        try {
            return objectString == null ? null : objectMapper.readValue(objectString, objectMapper.getTypeFactory()
                    .constructCollectionType(collectionClass, elementClass));
        } catch (JsonProcessingException e) {
            throw new ConversionException("Cannot convert response string '" + objectString + "' to Collection", e);
        }
    }

    @Override
    public <T> T toSpecified(TypeRef<T> typeRef) {
        Objects.requireNonNull(typeRef);
        JavaType javaType = objectMapper.constructType(typeRef.getType());
        try {
            return objectString == null ? null : objectMapper.readValue(objectString, javaType);
        } catch (JsonProcessingException e) {
            throw new ConversionException("Cannot convert response string '" + objectString + "' to '" + typeRef.getType() + "'", e);
        }
    }
}
