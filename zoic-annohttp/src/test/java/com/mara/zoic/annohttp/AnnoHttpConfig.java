package com.mara.zoic.annohttp;

import org.apache.hc.core5.http.HttpResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mara.zoic.annohttp.http.CoverableNameValuePair;
import com.mara.zoic.annohttp.http.HttpClientMetadata;
import com.mara.zoic.annohttp.http.PreparingRequest;
import com.mara.zoic.annohttp.http.response.converter.ResponseConverter;
import com.mara.zoic.annohttp.httpservice.TestClientWithBaseUriFunction;
import com.mara.zoic.annohttp.lifecycle.AnnoHttpLifecycle;

@Configuration
public class AnnoHttpConfig {

	@Bean
	public MyAnnoHttpLifecycle myAnnoHttpLifecycle() {
		return new MyAnnoHttpLifecycle();
	}
	
	
	class MyAnnoHttpLifecycle implements AnnoHttpLifecycle {

		@Override
		public void beforeClientCreating(Class<?> clientClass) {

		}

		@Override
		public void afterClientCreated(Object client) {

		}

		@Override
		public void beforeClientRequesting(HttpClientMetadata httpClientMetadata,
				PreparingRequest<?> preparingRequest) {
			if (TestClientWithBaseUriFunction.class == httpClientMetadata.getServiceClientClass()) {
				preparingRequest.customRequestHeaders(e -> e.add(new CoverableNameValuePair("X-Lifecycle-Spring", "Added")));
			}
		}

		@Override
		public void afterClientRequested(HttpClientMetadata httpClientMetadata, HttpResponse httpResponse,
				ResponseConverter responseConverter) {

		}
		
	}
}
