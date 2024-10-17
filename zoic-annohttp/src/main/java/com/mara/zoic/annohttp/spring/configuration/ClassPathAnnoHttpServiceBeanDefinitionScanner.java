package com.mara.zoic.annohttp.spring.configuration;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.lang.NonNull;

public class ClassPathAnnoHttpServiceBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public ClassPathAnnoHttpServiceBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilter) {
        super(registry, useDefaultFilter);
    }

    @Override
    protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
        return true;
    }
}
