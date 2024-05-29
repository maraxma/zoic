package com.mara.zoic.exloc.core;

import java.util.HashMap;
import java.util.Locale;

/**
 * 本地化的替换物。
 * <p>此类用于支持替换内容也需要本地化的情况。
 * <p>请使用构造函数构造此类，此类至少需要指定en-US的值，当找不到其他的值时，默认使用en-US的值。
 *
 * @author mm92
 * @since 1.3.3 2019-12-24
 */
public class LocalizedReplacement {

    private final HashMap<String, Object> replacements;
    private final String DEFAULT_LANGUAGE_TAG;

    /**
     * 构造一个本地化替换物。
     *
     * @param enUsValue 英文环境下的值，英文环境下的值会作为默认值存在，当找不到对应环境的值的时候会直接返回英文环境下的值
     */
    public LocalizedReplacement(Object enUsValue) {
        DEFAULT_LANGUAGE_TAG = Locale.US.toLanguageTag();
        replacements = new HashMap<String, Object>();
        replacements.put(DEFAULT_LANGUAGE_TAG, enUsValue);
    }

    /**
     * 构造一个本地化替换物。
     *
     * @param enUsValue 英文环境下的值
     * @param zhCnValue 简体中文环境下的值
     */
    public LocalizedReplacement(Object enUsValue, Object zhCnValue) {
        this(enUsValue);
        replacements.put(Locale.CHINA.toLanguageTag(), zhCnValue);
    }

    /**
     * 构造一个本地化替换物。
     *
     * @param enUsValue 英文环境下的值
     * @param zhCnValue 简体中文环境下的值
     * @param zhTwValue 繁体中文环境下的值
     */
    public LocalizedReplacement(Object enUsValue, Object zhCnValue, Object zhTwValue) {
        this(enUsValue, zhCnValue);
        replacements.put(Locale.TAIWAN.toLanguageTag(), zhTwValue);
    }

    /**
     * 为此替换物增加更多的值。
     *
     * @param languageTag 语言标签
     * @param value       在该环境下的值
     */
    public LocalizedReplacement lang(String languageTag, Object value) {
        replacements.put(languageTag, value);
        return this;
    }

    /**
     * 获得本地化的值。
     * <p>
     *
     * @param languageTag 语言标签
     * @return 本地化的值
     */
    public Object getLocalizedValue(String languageTag) {
        Object value = replacements.get(languageTag);
        if (value == null) {
            value = replacements.get(DEFAULT_LANGUAGE_TAG);
        }
        if (value == null) {
            // Unexpected
            throw new IllegalStateException("Cannot find default value from LocalizedReplacement for language tag [" + languageTag + "], current replacements are " + replacements);
        }
        return value;
    }
}
