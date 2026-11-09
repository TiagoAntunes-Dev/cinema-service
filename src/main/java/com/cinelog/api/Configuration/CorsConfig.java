package com.cinelog.api.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:4200,https://cinema-service-sv1b.onrender.com}")
            String[] allowedOrigins) {

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Vale para todas as rotas (API + Swagger)
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("Content-Type", "Accept", "X-API-Key", "X-Idempotency-Key", "X-API-Version")
                        .exposedHeaders("Location", "Retry-After", "X-RateLimit-Limit", "X-RateLimit-Remaining", "Idempotent-Replayed")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}