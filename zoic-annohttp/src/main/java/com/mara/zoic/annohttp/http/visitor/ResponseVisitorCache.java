package com.mara.zoic.annohttp.http.visitor;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

public class ResponseVisitorCache {

    private static final ConcurrentHashMap<Class<? extends ResponseVisitor>, ResponseVisitor> REG = new ConcurrentHashMap<>();

    public static void addVisitors(ResponseVisitor... responseVisitors) {

        for (ResponseVisitor responseVisitor : responseVisitors) {
            REG.put(responseVisitor.getClass(), responseVisitor);
        }
    }

    public static ResponseVisitor getOrCreate(Class<? extends ResponseVisitor> clazz) {
        return REG.compute(clazz, (k, v) -> {
            if (v == null) {
                try {
                    v = clazz.getDeclaredConstructor(new Class<?>[]{}).newInstance();
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                         | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    throw new RuntimeException("Cannot acquire ResponseVisitor for class '" + clazz + "'", e);
                }
            }
            return v;
        });
    }
}
