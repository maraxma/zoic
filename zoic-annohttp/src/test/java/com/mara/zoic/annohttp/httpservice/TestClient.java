package com.mara.zoic.annohttp.httpservice;

import com.mara.zoic.annohttp.annotation.AnnoHttpService;
import com.mara.zoic.annohttp.annotation.Query;
import com.mara.zoic.annohttp.annotation.Request;

@AnnoHttpService(baseUrl = "http://localhost:8081/")
public interface TestClient {
    @Request(url = "/test")
    String getItemName(@Query("ItemNo") String itemNo);
}
