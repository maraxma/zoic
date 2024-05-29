package com.mara.zoic.annohttp.http;

import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public abstract class AbstractStreamBasedConvertible implements Convertible, Closeable {

    protected InputStream inputStream;

    protected AbstractStreamBasedConvertible(ClassicHttpResponse httpResponse) {
        inputStream = Optional.ofNullable(httpResponse.getEntity()).map(e -> {
            try {
                return e.getContent();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }).orElse(null);
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
