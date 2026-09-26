package com.cinelog.api.Configuration; // Alinhado com o padrão de pacotes do seu projeto

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version:1.0.0}") String appVersion){
        return new OpenAPI()
                .info(new Info()
                        .title("Cinema Service API - Módulo de Usuários")
                        .version(appVersion)
                        .description("API REST para gerenciamento de usuários (Fase 1), com arquitetura em camadas e base para futura integração com catálogo de Filmes e Watchlist (Fase 2).")
                        .termsOfService("https://swagger.io/terms/")
                        .license(new License().name("MIT").url("https://mit-license.org/"))
                        .contact(new Contact().name("Tiago Antunes")
                                .url("")
                                .email("tiagoantunes1974@gmail.com"))
                );
    }
}
