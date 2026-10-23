package com.cinelog.api.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Aplica o CORS a todos os endpoints
                        .allowedOriginPatterns("*") // Permite origens com suporte a credenciais
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "TRACE") // Métodos permitidos
                        .allowedHeaders("*") // Cabeçalhos permitidos
                        .exposedHeaders("Authorization", "Link", "Location", "X-Total-Count") // Permite ler headers essenciais no front-end (HATEOAS e paginação)
                        .allowCredentials(true) // Permite credenciais e cookies
                        .maxAge(3600); // Cache do preflight por 1 hora
            }
        };
    }
}
