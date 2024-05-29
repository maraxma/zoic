package com.mara.zoic.annohttp.http.request.converter;


import com.mara.zoic.annohttp.http.HttpClientMetadata;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.FileEntity;

import java.io.File;

public class FileRequestBodyConverter implements RequestBodyConverter {

    public static final String FILE_FORM_FIELD_NAME = "FILE";

    @Override
    public HttpEntity convert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {
        HttpEntity httpEntity;
        if (contentType == null || (contentType != null && ContentType.APPLICATION_OCTET_STREAM.getMimeType().equalsIgnoreCase(contentType.getMimeType()))) {
            // 默认情况下为 APPLICATION_OCTET_STREAM
            httpEntity = new FileEntity((File) source, ContentType.APPLICATION_OCTET_STREAM);
        } else {
            // 否则只能指定为 MULTIPART_FORM_DATA
            if (ContentType.MULTIPART_FORM_DATA.getMimeType().equalsIgnoreCase(contentType.getMimeType())) {
                httpEntity = MultipartEntityBuilder.create()
                        .addBinaryBody("".equals(formFieldName) ? FILE_FORM_FIELD_NAME : formFieldName, (File) source)
                        .build();
            } else {
                throw new IllegalArgumentException("If @Body represent a File, its content type can application/octet-stream or multipart/form-data only, or you can not set content type, annohhttp set it as application/octet-stream by default");
            }
        }
        return httpEntity;
    }

    @Override
    public boolean canConvert(Object source, ContentType contentType,
                              HttpClientMetadata annoHttpClientMetadata, String formFieldName) {
        return source instanceof File;
    }

}
