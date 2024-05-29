package com.mara.zoic.annohttp.spring.configuration;


import com.mara.zoic.annohttp.annotation.AnnoHttpService;
import com.mara.zoic.annohttp.http.AnnoHttpClients;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

public class AnnoHttpServiceBeanFactoryProcessor implements BeanFactoryPostProcessor {

    private final String[] basePackages;

    public AnnoHttpServiceBeanFactoryProcessor(String[] basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        ClassPathAnnoHttpServiceBeanDefinitionScanner classPathAnnoHttpServiceBeanDefinitionScanner = new ClassPathAnnoHttpServiceBeanDefinitionScanner((BeanDefinitionRegistry) configurableListableBeanFactory, false);
        classPathAnnoHttpServiceBeanDefinitionScanner.addIncludeFilter(new AnnotationTypeFilter(AnnoHttpService.class));
        for (String basePackage : basePackages) {
            Set<BeanDefinition> found = classPathAnnoHttpServiceBeanDefinitionScanner.findCandidateComponents(basePackage);
            for (BeanDefinition bd : found) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    AnnoHttpService annoHttpServiceAnno = clazz.getAnnotation(AnnoHttpService.class);
                    if (annoHttpServiceAnno != null) {
                        String baseUrl = annoHttpServiceAnno.baseUrl();
                        Object httpService = AnnoHttpClients.create(clazz, baseUrl);
                        configurableListableBeanFactory.registerSingleton("annohttp-client-" + clazz.getName(), httpService);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
