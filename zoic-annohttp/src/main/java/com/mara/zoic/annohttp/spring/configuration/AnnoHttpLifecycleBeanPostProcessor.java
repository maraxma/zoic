package com.mara.zoic.annohttp.spring.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.mara.zoic.annohttp.http.AnnoHttpClients;
import com.mara.zoic.annohttp.lifecycle.AnnoHttpLifecycle;

public class AnnoHttpLifecycleBeanPostProcessor implements BeanPostProcessor {

	@Override
	@Nullable
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		if (AnnoHttpLifecycle.class.isAssignableFrom(bean.getClass())) {
			AnnoHttpClients.addAnnoHttpLifecycleInstances((AnnoHttpLifecycle) bean);
		}
		return bean;
	}

}
