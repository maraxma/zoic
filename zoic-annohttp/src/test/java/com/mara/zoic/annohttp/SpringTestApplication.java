package com.mara.zoic.annohttp;

import com.mara.zoic.annohttp.spring.annotation.EnableAnnoHttpAutoAssembling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAnnoHttpAutoAssembling
public class SpringTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringTestApplication.class, args);
    }
}