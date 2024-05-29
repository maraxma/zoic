package com.mara.zoic.exloc.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 基于XML的ResourceBundle的控制器。
 * @author MARA
 * @since 1.0.0 2022-01-13
 */
public class XmlResourceBundleControl extends ResourceBundle.Control {

    private static final String XML = "xml";

    public List<String> getFormats(String baseName) {
        return Collections.singletonList(XML);
    }

    public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                    ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException,
            IOException {

        if ((baseName == null) || (locale == null) || (format == null) || (loader == null)) {
            throw new NullPointerException();
        }

        if (!format.equals(XML)) {
            throw new IllegalArgumentException("Unknown format for XmlResourceBundle(.xml required): " + format);
        }

        ResourceBundle bundle;

        String bundleName = toBundleName(baseName, locale);
        String resourceName = toResourceName(bundleName, format);
        URL url = loader.getResource(resourceName);
        if (url == null) {
            return null;
        }
        URLConnection connection = url.openConnection();

        if (reload) {
            connection.setUseCaches(false);
        }
        try (InputStream stream = connection.getInputStream()) { // Mara.X.Ma 这里其实可以不用关闭 InputStream，因为下面的 new XmlResourceBundle 里面调用了 properties.loadFromXML，其会关闭 InputStream
            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                bundle = new DecoratedResourceBundle(new XmlResourceBundle(bis));
            }
        }

        return bundle;
    }
}
