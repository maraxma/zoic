package com.mara.zoic.annohttp.http;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.Serial;

/**
 * 首字母大写属性命名法。
 *
 * @author mm92
 * @since 1.0.6 2018-11-22
 */
public class JacksonCapitalizePropertyNamingStrategy extends PropertyNamingStrategies.NamingBase {

    @Serial
    private static final long serialVersionUID = -4940852506779479299L;

    @Override
    public String translate(String propertyName) {
        if (propertyName == null) {
            return null;
        }
        if (propertyName.isEmpty()) {
            return propertyName;
        }
        char c = propertyName.charAt(0);
        if (c >= 97 && c <= 122) {
            return Character.toTitleCase(c) +
                    propertyName.substring(1);
        }
        return propertyName;
    }
}
