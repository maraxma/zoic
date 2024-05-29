package com.mara.zoic.annohttp.http.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolHandlerMapping {

    private static final Map<String, ProtocolHandler> MAPPINGS = new ConcurrentHashMap<>();

    static {
        addMappings(new HttpProtocolHandler());
    }

    public static void addMappings(ProtocolHandler... protocolHandler) {
        if (protocolHandler == null) {
            return;
        }
        for (ProtocolHandler handler : protocolHandler) {
            if (handler == null) {
                continue;
            }
            String protocolStr = handler.protocol();
            String[] protocols = protocolStr.split(ProtocolHandler.PROTOCOL_SPLITTER);
            for (String protocol : protocols) {
                if (protocol != null && !"".equals((protocol = protocol.trim()))) {
                    if (MAPPINGS.containsKey(protocol)) {
                        throw new IllegalArgumentException("Protocol handler for protocol `" + protocol + "` is exist");
                    }
                    MAPPINGS.put(protocol, handler);
                }
            }
        }
    }

    public static ProtocolHandler getHandler(String protocol) {
        ProtocolHandler protocolHandler = MAPPINGS.get(protocol);
        if (protocolHandler == null) {
            throw new IllegalArgumentException("No handler for protocol `" + protocol + "`, please register it before using");
        }
        return protocolHandler;
    }
}
