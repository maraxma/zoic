package com.mara.zoic.exloc.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18nTest {

    @Test
    void testI18n() {
        XmlResourceBundleControl bundleControl = new XmlResourceBundleControl();
        ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n/testI18n", Locale.SIMPLIFIED_CHINESE, bundleControl);
        String rb_zh_cn = resourceBundle.getString("I0001");

        String lt_zh_cn = LocalizedTag.of("i18n/testI18n", "I0001").getLocalizedString(Locale.SIMPLIFIED_CHINESE);

        Assertions.assertEquals(rb_zh_cn, lt_zh_cn);

        String lt_enum_zh_cn = TestI18nCodes.I0001.getLocalizedString(Locale.SIMPLIFIED_CHINESE);

        Assertions.assertEquals(rb_zh_cn, lt_enum_zh_cn);

        String lt_new_zh_cn = new LocalizedTag.StringLocalizedTag("i18n/testI18n", "I0001").getLocalizedString(Locale.SIMPLIFIED_CHINESE);

        Assertions.assertEquals(rb_zh_cn, lt_new_zh_cn);
    }

    enum TestI18nCodes implements LocalizedTag {

        I0001;

        @Override
        public String tag() {
            return "i18n/testI18n" + ":" + name();
        }
    }
}
