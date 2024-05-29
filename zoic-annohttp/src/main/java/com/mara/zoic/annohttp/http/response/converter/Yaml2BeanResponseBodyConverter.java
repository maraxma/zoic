package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.JacksonComponentHolder;
import org.apache.hc.core5.http.ContentType;

import java.util.Set;

public class Yaml2BeanResponseBodyConverter extends AbstractJackson2BeanResponseBodyConverter {

    public Yaml2BeanResponseBodyConverter() {
        super();
        objectMapper = JacksonComponentHolder.getJsonMapper(false, true, true, false);
        acceptableContentTypes = Set.of(ContentType.create("application/yml"), ContentType.create("application/yaml"),
                ContentType.create("text/yml"), ContentType.create("text/yaml"));
        name = "yaml";
    }
}
