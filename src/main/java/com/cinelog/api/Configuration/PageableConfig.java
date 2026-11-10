package com.cinelog.api.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PageableConfig {

    @Bean
public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
    return resolver -> {
        resolver.setMaxPageSize(50); // Limite máximo
        resolver.setFallbackPageable(PageRequest.of(0, 20)); // Padrão se não for enviado
    };
}
}