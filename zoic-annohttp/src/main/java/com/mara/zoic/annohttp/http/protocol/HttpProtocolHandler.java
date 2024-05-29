package com.mara.zoic.annohttp.http.protocol;

import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.PreparingRequest;

public final class HttpProtocolHandler implements ProtocolHandler {

    @Override
    public String protocol() {
        return "http" + PROTOCOL_SPLITTER +  "https";
    }

    @Override
    public void handle(HttpClientMetadata metadata, PreparingRequest<?> preparingRequest) {

    }
}
