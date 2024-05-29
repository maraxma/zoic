package com.mara.zoic.annohttp.spring.configuration;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public class AnnoHttpBeanFactoryProcessorConfiguration {

    @Bean
    public static AnnoHttpServiceBeanFactoryProcessor annotHttpServiceBeanFactoryProcessor(Environment environment) {
        AnnoHttpProperties annoHttpProperties = Binder.get(environment).bind(AnnoHttpProperties.NAMESPACE, Bindable.of(AnnoHttpProperties.class)).get();
        return new AnnoHttpServiceBeanFactoryProcessor(annoHttpProperties.getServiceBasePackages());
    }
}
