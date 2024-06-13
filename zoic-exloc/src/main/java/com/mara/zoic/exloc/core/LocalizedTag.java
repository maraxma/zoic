package com.mara.zoic.exloc.core;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 代表一个本地化字符串的唯一代码标识。
 * <p>一般来说此接口应该配合枚举来使用，使枚举实现此接口，然后使用单个枚举项来获取本地化字符串（推荐）。</p>
 * <p>此接口也可以作为工具来直接获取本地化字符串（便捷使用，在结构化开发中更推荐前面的使用方式）。</p>
 * <p>此接口可以作为函数式接口来使用。</p>
 * @author MARA
 * @since 1.0.0 2022-01-13
 */
@FunctionalInterface
public interface LocalizedTag {

    ResourceBundle.Control CONTROL = new XmlResourceBundleControl();

    /**
     * 获得此接口实例使用的 {@link ResourceBundle.Control} 对象。
     * <p>默认的实现会使用 {@link XmlResourceBundleControl}。此 Control 支持XML文件，相对于 properties 文件，
     * XML文件虽然体积稍大但是排版清晰，支持UTF8编码，对于开发来说很方便。可以重写此方法让 {@link LocalizedTag} 使用自定义的 {@link ResourceBundle.Control}。</p>
     * <p>{@link ResourceBundle} 本身提供了支持properties文件的 {@link ResourceBundle.Control}。</p>
     * @return {@link ResourceBundle.Control} 对象
     */
    default ResourceBundle.Control getResourceBundleControl() {
        return CONTROL;
    }

    /**
     * 获得此对象的Tag，该Tag代表的是在本地化过程中使用的唯一标识。
     * @return 本地化唯一标识，标识应该是 {@code LocalizedFamily:Key} 这种形式，其中的“LocalizedFamily”代表本地化家族，即一个完整的ResourceBundle资源。
     * “Key”则代表的是具体的本地化字符串的唯一标识
     */
    String tag();

    /**
     * 返回此Tag所对应的本地化字符串。
     * @param locale 当地地区
     * @param replacements 占位符替换物（String.format）
     */
    default String getLocalizedString(Locale locale, Object... replacements) {

        String[] detailedTag = tag().split(":");
        if (detailedTag.length != 2) {
            throw new IllegalArgumentException("Illegal Localized Tag: " + tag() + " (tag should be a string like 'LocalizedFamily:Key')");
        }

        String localeFamily = detailedTag[0].trim();
        String key = detailedTag[1].trim();
        String originalString = ResourceBundle.getBundle(localeFamily, locale, getResourceBundleControl()).getString(key);

        Object[] finalReplacements = new Object[replacements.length];
        for (int i = 0; i < replacements.length; i++) {
            if (replacements[i] instanceof LocalizedReplacement) {
                // LocalizedReplacement需要特殊处理
                finalReplacements[i] =
                        ((LocalizedReplacement) replacements[i]).getLocalizedValue(locale.toLanguageTag());
            } else {
                finalReplacements[i] = replacements[i];
            }
        }
        return String.format(originalString, finalReplacements);
    }

    /**
     * 自动获取locale，返回此Tag所对应的本地化字符串。
     * <p>Locale是通过Spring提供的 {@link org.springframework.context.i18n.LocaleContextHolder LocalContextHolder} 来获取的。
     * 在非Spring环境下无效，请勿在非Spring环境下使用此方法，否则达不到预期的效果且如果没有spring-context依赖的话会抛出异常。</p>
     * @param replacements 占位符替换物（String.format）
     * @see org.springframework.context.i18n.LocaleContextHolder
     */
    default String getLocalizedString(Object... replacements) {
        return getLocalizedString(LocaleContextHolder.getLocale(), replacements);
    }

    static LocalizedTag of(String localeFamily, String tag) {
        if (localeFamily == null || tag == null || localeFamily.isEmpty() || tag.isEmpty()) {
            throw new IllegalArgumentException("localeFamily and tag cannot be null or empty");
        }
        return new StringLocalizedTag(localeFamily, tag);
    }

    /**
     * 字符串形式的本地化标识。
     * @author MARA
     * @since 1.0.0 2022-01-13
     */
    class StringLocalizedTag implements LocalizedTag {

        private final String localeFamily;
        private final String tag;

        public StringLocalizedTag(String localeFamily, String tag) {
            this.localeFamily = localeFamily;
            this.tag = tag;
        }

        @Override
        public String tag() {
            return localeFamily + ":" + tag;
        }
    }
}
