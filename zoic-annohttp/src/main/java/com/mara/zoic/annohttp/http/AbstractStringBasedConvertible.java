package com.mara.zoic.annohttp.http;


import com.mara.zoic.annohttp.http.exception.ConversionException;
import org.apache.hc.core5.http.ClassicHttpResponse;

/**
 * 基于字符串的可转换对象。
 *
 * @author Mara.X.Ma
 * @since 1.0.0
 */
public abstract class AbstractStringBasedConvertible extends AbstractByteArrayBasedConvertible {

    protected final String objectString;

    private static final String DEFAULT_CHARSET = "UTF-8";

    protected AbstractStringBasedConvertible(ClassicHttpResponse httpResponse, String charset) {
        super(httpResponse);
        String finalCharset = charset == null ? DEFAULT_CHARSET : charset;
        try {
            if (bytes == null) {
                objectString = null;
            } else {
                objectString = new String(bytes, finalCharset);
            }
        } catch (Exception e) {
            throw new ConversionException("Cannot convert bytes to String with charset '" + finalCharset + "'", e);
        }
    }

    /**
     * 直接返回该对象的字符串形式。
     * @return 字符串
     */
    @Override
    public String toString() {
        return objectString;
    }
}
