package com.cinelog.api.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PageableConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            // Define o limite máximo de itens por página
            resolver.setMaxPageSize(50); 
            // Opcional: define o tamanho padrão se o cliente não enviar o parâmetro
            // resolver.setFallbackPageable(PageRequest.of(0, 20)); 
        };
    }
}