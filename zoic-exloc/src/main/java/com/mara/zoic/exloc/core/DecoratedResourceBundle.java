package com.mara.zoic.exloc.core;

import org.jetbrains.annotations.NotNull;

import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 * 代表一个经过扩展的ResourceBundle。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-01-13
 */
public class DecoratedResourceBundle extends ResourceBundle {

    private final ResourceBundle resourceBundle;

    DecoratedResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @Override
    protected Object handleGetObject(@NotNull String key) {
        return resourceBundle.getObject(key);
    }


    @Override
    public Enumeration<String> getKeys() {
        return resourceBundle.getKeys();
    }

    public String getStringByEnumName(Enum<?> en) {
        return resourceBundle.getString(en.name());
    }

    public String getStringByEnumToString(Enum<?> en) {
        return resourceBundle.getString(en.toString());
    }

    public Object getObjectByEnumName(Enum<?> en) {
        return resourceBundle.getObject(en.name());
    }

    public Object getObjectByEnumToString(Enum<?> en) {
        return resourceBundle.getObject(en.toString());
    }

    public String[] getStringArrayByEnumName(Enum<?> en) {
        return resourceBundle.getStringArray(en.name());
    }

    public String[] getStringArrayByEnumToString(Enum<?> en) {
        return resourceBundle.getStringArray(en.toString());
    }

    public String getString(LocalizedTag tag) {
        return resourceBundle.getString(tag.tag());
    }

    public Object getObject(LocalizedTag tag) {
        return resourceBundle.getObject(tag.tag());
    }

    public String[] getStringArray(LocalizedTag tag) {
        return resourceBundle.getStringArray(tag.tag());
    }
}
