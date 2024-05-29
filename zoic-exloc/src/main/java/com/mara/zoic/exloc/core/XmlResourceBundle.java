package com.mara.zoic.exloc.core;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * 描述一个基于XML文件的ResourceBundle。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-01-13
 */
public class XmlResourceBundle extends ResourceBundle {

    private final Properties properties;

    public XmlResourceBundle(InputStream xmlInputStream) {
        properties = new Properties();
        try {
            properties.loadFromXML(xmlInputStream); // Method 'loadFromXML' will close InputStream itself.
        } catch (IOException e) {
            throw new RuntimeException("Cannot init XmlResourceBundle", e);
        }
    }

    @Override
    protected Object handleGetObject(@NotNull String key) {
        return properties.getProperty(key);
    }

    @NotNull
    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(properties.stringPropertyNames());
    }
}
