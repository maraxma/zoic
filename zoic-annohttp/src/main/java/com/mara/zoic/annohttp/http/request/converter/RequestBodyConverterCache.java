package com.mara.zoic.annohttp.http.request.converter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RequestBodyConverterCache {

    static final LinkedHashMap<Class<? extends RequestBodyConverter>, RequestBodyConverter> REG_MAP = new LinkedHashMap<>();

    static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    static final Map<Class<? extends RequestBodyConverter>, RequestBodyConverter> DEFAULT_REG_MAP;

    public static final RequestBodyConverter AUTO_REQUEST_BODY_CONVERTER = new AutoRequestBodyConverter();

    static {
        LinkedHashMap<Class<? extends RequestBodyConverter>, RequestBodyConverter> map = new LinkedHashMap<>();
        map.put(StringRequestBodyConverter.class, new StringRequestBodyConverter());
        map.put(MapRequestBodyConverter.class, new MapRequestBodyConverter());
        map.put(ByteArrayRequestBodyConverter.class, new ByteArrayRequestBodyConverter());
        map.put(FileRequestBodyConverter.class, new FileRequestBodyConverter());
        map.put(CommonJavaObjectRequestBodyConverter.class, new CommonJavaObjectRequestBodyConverter());
        DEFAULT_REG_MAP = Collections.unmodifiableMap(map);
    }

    public static void addUserConverters(RequestBodyConverter... converters) {
        Lock lock = LOCK.writeLock();
        try {
            lock.lock();
            for (RequestBodyConverter requestBodyConverter : converters) {
                REG_MAP.put(requestBodyConverter.getClass(), requestBodyConverter);
            }
        } finally {
            lock.unlock();
        }
    }

    public static Map<Class<? extends RequestBodyConverter>, RequestBodyConverter> getAll() {
        Lock lock = LOCK.readLock();
        try {
            lock.lock();
            LinkedHashMap<Class<? extends RequestBodyConverter>, RequestBodyConverter> map = new LinkedHashMap<>();
            map.putAll(REG_MAP);
            map.putAll(DEFAULT_REG_MAP);
            return Collections.unmodifiableMap(map);
        } finally {
            lock.unlock();
        }
    }
}
