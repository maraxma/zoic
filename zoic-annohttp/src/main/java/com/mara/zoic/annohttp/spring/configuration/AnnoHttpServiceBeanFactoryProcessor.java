package com.mara.zoic.annohttp.spring.configuration;


import com.mara.zoic.annohttp.annotation.AnnoHttpService;
import com.mara.zoic.annohttp.http.AnnoHttpClients;
import com.mara.zoic.annohttp.http.HttpClientMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;

import java.util.Set;
import java.util.function.Function;

public class AnnoHttpServiceBeanFactoryProcessor implements BeanFactoryPostProcessor {

    private final String[] basePackages;

    private Logger logger = LoggerFactory.getLogger(AnnoHttpServiceBeanFactoryProcessor.class);

    public AnnoHttpServiceBeanFactoryProcessor(String[] basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        ClassPathAnnoHttpServiceBeanDefinitionScanner classPathAnnoHttpServiceBeanDefinitionScanner = new ClassPathAnnoHttpServiceBeanDefinitionScanner((BeanDefinitionRegistry) configurableListableBeanFactory, false);
        classPathAnnoHttpServiceBeanDefinitionScanner.addIncludeFilter(new AnnotationTypeFilter(AnnoHttpService.class));
        for (String basePackage : basePackages) {
            int scanned = 0;
            Set<BeanDefinition> found = classPathAnnoHttpServiceBeanDefinitionScanner.findCandidateComponents(basePackage);
            for (BeanDefinition bd : found) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    AnnoHttpService annoHttpServiceAnno = clazz.getAnnotation(AnnoHttpService.class);
                    Object httpService;
                    if (annoHttpServiceAnno != null) {
                        Class<? extends Function<HttpClientMetadata, String>> baseUrlFunctionClass = annoHttpServiceAnno.baseUrlFunctionClass();
                        if (baseUrlFunctionClass != AnnoHttpService.EmptyBaseUrlFunction.class) {
                            Function<HttpClientMetadata, String> baseUrlFunction;
                            try {
                                baseUrlFunction = baseUrlFunctionClass.getDeclaredConstructor(new Class[]{}).newInstance();
                                httpService = AnnoHttpClients.create(clazz, baseUrlFunction);
                            } catch (Exception e) {
                                throw e;
                            }
                        } else {
                            String baseUrl = annoHttpServiceAnno.baseUrl();
                            if (!"".equals(baseUrl)) {
                                httpService = AnnoHttpClients.create(clazz, baseUrl);
                            } else {
                                httpService = AnnoHttpClients.create(clazz);
                            }
                        }
                        configurableListableBeanFactory.registerSingleton("annohttp-client-" + clazz.getName(), httpService);
                        scanned++;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (scanned == 0) {
                if (logger.isWarnEnabled()) {
                    logger.warn("No AnnoHttpService found in package: {}", basePackage);
                }
            }
        }
    }
}
