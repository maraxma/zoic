package com.mara.zoic.annohttp.http;


import com.mara.zoic.annohttp.http.exception.ConversionException;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 可操作响应体。提供一系列方便的操作方法以便将此响应体转换为用户期望的形式。
 * @param httpResponse 响应体实例
 * @author Mara.X.Ma
 * @since 1.0.0
 */
public record OperableHttpResponse(ClassicHttpResponse httpResponse) implements ClassicHttpResponse, Sequencable {

    public OperableHttpResponse {
        if (httpResponse == null) {
            throw new IllegalArgumentException("httpResponse cannot be null");
        }
    }

    @Override
    public void setVersion(ProtocolVersion version) {
    	httpResponse.setVersion(version);
    }

    @Override
    public ProtocolVersion getVersion() {
        return httpResponse.getVersion();
    }

    @Override
    public boolean containsHeader(String name) {
        return httpResponse.containsHeader(name);
    }

    @Override
    public int countHeaders(String name) {
        return httpResponse.countHeaders(name);
    }

    @Override
    public Header[] getHeaders(String name) {
        return httpResponse.getHeaders(name);
    }

    @Override
    public Header getFirstHeader(String name) {
        return httpResponse.getFirstHeader(name);
    }

    @Override
    public Header getHeader(String name) throws ProtocolException {
        return httpResponse.getHeader(name);
    }

    @Override
    public Header getLastHeader(String name) {
        return httpResponse.getLastHeader(name);
    }

    @Override
    public Iterator<Header> headerIterator() {
        return httpResponse.headerIterator();
    }

    @Override
    public Iterator<Header> headerIterator(String name) {
        return httpResponse.headerIterator(name);
    }

    @Override
    public Header[] getHeaders() {
        return httpResponse.getHeaders();
    }

    @Override
    public void addHeader(Header header) {
        httpResponse.addHeader(header);
    }

    @Override
    public void addHeader(String name, Object value) {
    	httpResponse.addHeader(name, value);
    }

    @Override
    public void setHeader(Header header) {
    	httpResponse.setHeader(header);
    }

    @Override
    public void setHeader(String name, Object value) {
    	httpResponse.setHeader(name, value);
    }

    @Override
    public void setHeaders(Header... headers) {
    	httpResponse.setHeaders(headers);
    }

    @Override
    public boolean removeHeader(Header header) {
        return httpResponse.removeHeader(header);
    }

    @Override
    public boolean removeHeaders(String name) {
    	return httpResponse.removeHeaders(name);
    }


    @Override
    public String asSequenceToString() {
        return asSequenceToString(DEFAULT_CHARSET);
    }

    @Override
    public String asSequenceToString(String charset) {
        try {
            byte[] bytes = EntityUtils.toByteArray(httpResponse.getEntity());
            if (bytes == null) {
                return null;
            }
            return new String(bytes, charset);
        } catch (Exception e) {
            throw new ConversionException("Response body cannot convert to String whit charset '" + charset + "'", e);
        }
    }

    @Override
    public byte[] asSequenceToBytes() {
        try {
            return EntityUtils.toByteArray(httpResponse.getEntity());
        } catch (IOException e) {
            throw new ConversionException("Response body cannot convert to byte[]", e);
        }
    }

    @Override
    public InputStream asInputStream() {
        return Optional.of(httpResponse.getEntity()).map(t -> {
            try {
                return t.getContent();
            } catch (UnsupportedOperationException | IOException e) {
                throw new ConversionException("Cannot acquire InputStream from response", e);
            }
        }).orElse(null);
    }

    @Override
    public Object asJavaSerializedSequenceToObject() {
        try (InputStream inputStream = asInputStream()) {
            if (inputStream == null) {
                return null;
            }
            try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                return objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new ConversionException("Cannot convert response to Java Object", e);
            }
        } catch (IOException e) {
            throw new ConversionException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T asJavaSerializedSequenceToObject(Class<T> objectClass) {
        return (T) asJavaSerializedSequenceToObject();
    }

    @Override
    public Convertible asXmlConvertible() {
        return new XmlConvertible(httpResponse, DEFAULT_CHARSET);
    }

    @Override
    public Convertible asJsonConvertible() {
        return new JsonConvertible(httpResponse, DEFAULT_CHARSET);
    }

    @Override
    public Convertible asYamlConvertible() {
        return new YamlConvertible(httpResponse, DEFAULT_CHARSET);
    }

    @Override
    public Convertible asConvertible(BiFunction<ClassicHttpResponse, String, Convertible> convertibleProducer) {
        if (convertibleProducer == null) {
            return null;
        }
        return convertibleProducer.apply(httpResponse, DEFAULT_CHARSET);
    }

    @Override
    public void close() {
        EntityUtils.consumeQuietly(getEntity());
    }

    @Override
    public HttpEntity getEntity() {
        return httpResponse.getEntity();
    }

    @Override
    public void setEntity(HttpEntity entity) {
        httpResponse.setEntity(entity);
    }

    @Override
    public int getCode() {
        return httpResponse.getCode();
    }

    @Override
    public void setCode(int code) {
        httpResponse.setCode(code);
    }

    @Override
    public String getReasonPhrase() {
        return httpResponse.getReasonPhrase();
    }

    @Override
    public void setReasonPhrase(String reason) {
        httpResponse.setReasonPhrase(reason);
    }

    @Override
    public Locale getLocale() {
        return httpResponse.getLocale();
    }

    @Override
    public void setLocale(Locale loc) {
        httpResponse.setLocale(loc);
    }
}
