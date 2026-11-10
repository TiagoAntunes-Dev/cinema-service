package com.cinelog.api.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest; // <-- ADICIONE ESTA LINHA
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PageableConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            // Define o limite máximo de itens por página
            resolver.setMaxPageSize(50); 
            // Define o padrão de 20 itens se o cliente não enviar nada
            resolver.setFallbackPageable(PageRequest.of(0, 20)); 
        };
    }
}