package com.mara.zoic.annohttp.http.response.converter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ResponseConverterCache {

    static final Map<Class<? extends ResponseConverter>, ResponseConverter> REG_MAP = new LinkedHashMap<>();

    static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    static final Map<Class<? extends ResponseConverter>, ResponseConverter> DEFAULT_REG_MAP;

    public static final ResponseConverter AUTO_RESPONSE_BODY_CONVERTER = new AutoResponseConverter();

    static {
        LinkedHashMap<Class<? extends ResponseConverter>, ResponseConverter> map = new LinkedHashMap<>();
        map.put(StatusLineResponseConverter.class, new StatusLineResponseConverter());
        map.put(ResponseHeaderResponseConverter.class, new ResponseHeaderResponseConverter());
        map.put(InputStreamResponseBodyConverter.class, new InputStreamResponseBodyConverter());
        map.put(StringResponseBodyConverter.class, new StringResponseBodyConverter());
        map.put(ByteArrayResponseBodyConverter.class, new ByteArrayResponseBodyConverter());
        map.put(HttpResponseResponseConverter.class, new HttpResponseResponseConverter());
        map.put(Json2BeanResponseBodyConverter.class, new Json2BeanResponseBodyConverter());
        map.put(Xml2BeanResponseBodyConverter.class, new Xml2BeanResponseBodyConverter());
        map.put(Yaml2BeanResponseBodyConverter.class, new Yaml2BeanResponseBodyConverter());
        map.put(InputStream2JavaObjectResponseBodyConverter.class, new InputStream2JavaObjectResponseBodyConverter());
        DEFAULT_REG_MAP = Collections.unmodifiableMap(map);
    }

    public static void addUserConverters(ResponseConverter... converters) {
        Lock lock = LOCK.writeLock();
        try {
            lock.lock();
            for (ResponseConverter responseConverter : converters) {
                REG_MAP.put(responseConverter.getClass(), responseConverter);
            }
        } finally {
            lock.unlock();
        }
    }

    public static Map<Class<? extends ResponseConverter>, ResponseConverter> getAll() {
        Lock lock = LOCK.readLock();
        try {
            lock.lock();
            LinkedHashMap<Class<? extends ResponseConverter>, ResponseConverter> map = new LinkedHashMap<>();
            map.putAll(REG_MAP);
            map.putAll(DEFAULT_REG_MAP);
            return Collections.unmodifiableMap(map);
        } finally {
            lock.unlock();
        }
    }
}
