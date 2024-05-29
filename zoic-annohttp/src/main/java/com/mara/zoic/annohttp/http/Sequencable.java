package com.mara.zoic.annohttp.http;

import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.Closeable;
import java.io.InputStream;
import java.util.function.BiFunction;

/**
 * 代表一个可以视为序列的对象。
 */
public interface Sequencable extends Closeable {

    String DEFAULT_CHARSET = "UTF-8";

    /**
     * 当做输入流返回。
     * @return 输入流
     */
    InputStream asInputStream();

    /**
     * 当做字符串返回。字符串采用 {@link String#String(byte[], String)} 构造，并使用默认的UTF-8编码。
     * @return 字符串
     */
    String asSequenceToString();

    /**
     * 当做字符串返回。字符串采用 {@link String#String(byte[], String)} 构造，并使用指定的字符编码。
     * @param charset 字符编码
     * @return 字符串
     */
    String asSequenceToString(String charset);

    /**
     * 当做字节数组返回。
     * @return 字节数组
     */
    byte[] asSequenceToBytes();

    /**
     * 当做Java序列化后的Object返回。
     * @return 采用Java反序列化后的对象
     */
    Object asJavaSerializedSequenceToObject();

    /**
     * 当做Java序列化后的Object返回。
     * @param objectClass 对象的类型
     * @return 采用Java反序列化后的对象
     * @param <T> 对象的类型
     */
    <T> T asJavaSerializedSequenceToObject(Class<T> objectClass);

    /**
     * 当做 XML 内容的可转换对象返回。
     * @return XML内容的可转换对象
     */
    Convertible asXmlConvertible();

    /**
     * 当做 JSON 内容的可转换对象返回。
     * @return JSON内容的可转换对象
     */
    Convertible asJsonConvertible();

    /**
     * 当做 YAML 内容的可转换对象返回。
     * @return YAML内容的可转换对象
     */
    Convertible asYamlConvertible();

    /**
     * 当做自定义对象的可转换对象返回。
     * @param convertibleProducer 转换器提供器
     * @return 自定义对象的可转换对象
     * @see Convertible
     */
    Convertible asConvertible(BiFunction<ClassicHttpResponse, String, Convertible> convertibleProducer);

}
