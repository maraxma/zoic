package com.mara.zoic.annohttp.httpservice;

import com.mara.zoic.annohttp.annotation.AnnoHttpService;
import com.mara.zoic.annohttp.annotation.Query;
import com.mara.zoic.annohttp.annotation.Request;
import com.mara.zoic.annohttp.http.HttpClientMetadata;
import org.apache.hc.core5.http.Header;

import java.util.function.Function;

@AnnoHttpService(baseUriFunctionClass = TestClientWithBaseUriFunction.MyBaseUriFunction.class)
public interface TestClientWithBaseUriFunction {

    @Request(uri = "/test")
    Header[] getItemName(@Query("ItemNo") String itemNo);

    class MyBaseUriFunction implements Function<HttpClientMetadata, String> {
        @Override
        public String apply(HttpClientMetadata metadata) {
            String arg0 = (String) metadata.getRequestMethodArguments()[0];
            if ("Special".equals(arg0)) {
                return "http://localhost:9081/";
            }
            return "http://localhost:8081/";
        }
    }
}
