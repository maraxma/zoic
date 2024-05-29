package com.mara.zoic.annohttp.testsup;

import com.mara.zoic.annohttp.http.PreparingRequest;

/**
 * 用于存储PreparingRequest实例。当这个类出现在请求方法的参数列表中时，会自动注入一个PreparingRequest实例。
 */
public class PreparingRequestContainer {
    private PreparingRequest<?> preparingRequest;

    public PreparingRequest<?> getPreparingRequest() {
        return preparingRequest;
    }

    public void setPreparingRequest(PreparingRequest<?> preparingRequest) {
        this.preparingRequest = preparingRequest;
    }
}
