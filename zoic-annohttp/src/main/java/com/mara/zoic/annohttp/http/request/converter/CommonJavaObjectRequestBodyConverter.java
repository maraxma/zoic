package com.mara.zoic.annohttp.http.request.converter;


import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.JacksonComponentHolder;
import com.mara.zoic.annohttp.http.exception.ConversionException;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

public class CommonJavaObjectRequestBodyConverter implements RequestBodyConverter {

    @Override
    public HttpEntity convert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {

        HttpEntity httpEntity;
        JsonMapper jsonMapper = JacksonComponentHolder.getJsonMapper(false, true, true, false);
        XmlMapper xmlMapper = JacksonComponentHolder.getXmlMapper(false, true, true, false);
        YAMLMapper yamlMapper = JacksonComponentHolder.getYamlMapper(false, true, true, false);
        String tag = "";

        try {
            if (contentType == null) {
                tag = "json";
                httpEntity = new StringEntity(jsonMapper.writeValueAsString(source), ContentType.APPLICATION_JSON);
            } else {
                final Charset charset = contentType.getCharset() == null ? DEFAULT_STRING_CHARSET : contentType.getCharset();
                String userMimeType = contentType.getMimeType();
                if (ContentType.APPLICATION_JSON.getMimeType().equalsIgnoreCase(userMimeType)) {
                    tag = "json";
                    httpEntity = new StringEntity(jsonMapper.writeValueAsString(source), contentType.withCharset(charset));
                } else if (ContentType.APPLICATION_XML.getMimeType().equalsIgnoreCase(userMimeType)
                        || ContentType.TEXT_XML.getMimeType().equalsIgnoreCase(userMimeType)) {
                    tag = "xml";
                    httpEntity = new StringEntity(xmlMapper.writeValueAsString(source), contentType.withCharset(charset));
                } else if (CONTENT_TYPE_APPLICATION_YAML.equalsIgnoreCase(userMimeType)
                        || CONTENT_TYPE_APPLICATION_YML.equalsIgnoreCase(userMimeType)
                        || CONTENT_TYPE_TEXT_YAML.equalsIgnoreCase(userMimeType)
                        || CONTENT_TYPE_TEXT_YML.equalsIgnoreCase(userMimeType)) {
                    tag = "yaml";
                    httpEntity = new StringEntity(yamlMapper.writeValueAsString(source), contentType.withCharset(charset));
                } else if (ContentType.TEXT_PLAIN.getMimeType().equalsIgnoreCase(userMimeType)) {
                    tag = "text";
                    httpEntity = new StringEntity(String.valueOf(source), contentType.withCharset(charset));
                } else if (ContentType.APPLICATION_OCTET_STREAM.getMimeType().equalsIgnoreCase(userMimeType)) {
                    // java序列化
                    if (source instanceof Serializable) {
                        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                            objectOutputStream.writeObject(source);
                            httpEntity = new InputStreamEntity(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), contentType.withCharset(charset));
                        }
                    } else {
                        throw new IllegalArgumentException("If @Body represent a common java object and content type is stream, the java object must be an instance of Serializable so annohttp can convert it to stream. You can write your own RequestBodyConverter to process it");
                    }
                } else {
                    throw new IllegalArgumentException("If @Body represent a common java object, its content type can json | xml | yaml | stream | text only, or you can not set content type, annohhttp set it as application/json by default");
                }
            }
        } catch (Exception e) {
            throw new ConversionException(this, "Cannot convert map to " + tag, e);
        }

        return httpEntity;
    }

    @Override
    public boolean canConvert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {
        return true;
    }

}
