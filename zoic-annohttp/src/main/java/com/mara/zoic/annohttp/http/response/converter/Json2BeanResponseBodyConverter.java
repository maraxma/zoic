package com.mara.zoic.annohttp.http.response.converter;


import com.mara.zoic.annohttp.http.JacksonComponentHolder;
import org.apache.hc.core5.http.ContentType;

import java.util.Set;

public class Json2BeanResponseBodyConverter extends AbstractJackson2BeanResponseBodyConverter {

    public Json2BeanResponseBodyConverter() {
        super();
        objectMapper = JacksonComponentHolder.getJsonMapper(false, true, true, false);
        acceptableContentTypes = Set.of(ContentType.APPLICATION_JSON, ContentType.create("text/json"));
        name = "json";
    }

}
