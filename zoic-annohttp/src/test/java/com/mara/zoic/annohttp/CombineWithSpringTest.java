package com.mara.zoic.annohttp;

import com.mara.zoic.annohttp.httpservice.TestClient;
import com.mara.zoic.annohttp.httpservice.TestClientWithoutAnno;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SpringTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class CombineWithSpringTest {

    @Autowired(required = false)
    private TestClient testClient;

    @Autowired(required = false)
    private TestClientWithoutAnno testClientWithoutAnno;

    @Test
    void scanTest() {
        Assertions.assertNotNull(testClient);
        Assertions.assertNull(testClientWithoutAnno);
    }

}
