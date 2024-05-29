package com.mara.zoic.annohttp.http;


import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.Serial;

public class CoverableNameValuePair extends BasicNameValuePair {

    @Serial
    private static final long serialVersionUID = -6809721642742357316L;

    private final boolean coverable;

    public CoverableNameValuePair(String name, String value, boolean coverable) {
        super(name, value);
        this.coverable = coverable;
    }

    public CoverableNameValuePair(String name, String value) {
        this(name, value, false);
    }

    public boolean isCoverable() {
        return coverable;
    }
}
