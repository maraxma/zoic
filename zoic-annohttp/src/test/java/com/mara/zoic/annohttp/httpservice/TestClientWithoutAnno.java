package com.mara.zoic.annohttp.httpservice;

import com.mara.zoic.annohttp.annotation.Query;
import com.mara.zoic.annohttp.annotation.Request;

public interface TestClientWithoutAnno {
    @Request(url = "https://baidu.com")
    String getItemName(@Query("ItemNo") String itemNo);
}
