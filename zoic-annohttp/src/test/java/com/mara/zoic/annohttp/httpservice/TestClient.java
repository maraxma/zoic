package com.mara.zoic.annohttp.httpservice;

import com.mara.zoic.annohttp.annotation.AnnoHttpService;
import com.mara.zoic.annohttp.annotation.Query;
import com.mara.zoic.annohttp.annotation.Request;

@AnnoHttpService(baseUri = "http://localhost:8081/")
public interface TestClient {
    @Request(uri = "/test")
    String getItemName(@Query("ItemNo") String itemNo);
}
