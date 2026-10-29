package com.cinelog.api.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig: política de CORS (Cross-Origin Resource Sharing) da API.
 *
 * O que é: por segurança, o navegador bloqueia que uma página de uma origem (ex.: http://localhost:3000)
 * leia respostas de outra origem (ex.: http://localhost:8080) a menos que a API autorize.
 * Origem = protocolo + domínio + porta. A autorização vem em cabeçalhos "Access-Control-Allow-*".
 *
 * O que esta classe define:
 * - ORIGENS: só as listadas em "cors.allowed-origins" (application.properties). Nada de "*".
 * - MÉTODOS: só os métodos HTTP que a API realmente usa.
 * - CABEÇALHOS: só os que o cliente precisa enviar.
 * - Requisição vinda de uma origem NÃO listada é recusada com 403 (Invalid CORS request).
 *
 * Importante: o CORS só vale para navegadores. Postman, curl e o próprio Swagger UI
 * (mesma origem da API) não são afetados.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(
            // Lista separada por vírgula. Se a propriedade não existir, usa os servidores de
            // desenvolvimento mais comuns de front-end (React/Next, Vite e Angular).
            @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:4200}")
            String[] allowedOrigins) {

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Vale para todas as rotas da API
                        .allowedOrigins(allowedOrigins) // Origens específicas (sem "*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Métodos permitidos
                        // Cabeçalhos que o cliente pode enviar. Os "X-..." já ficam liberados
                        // para a Parte 2 (API Key, idempotência e versionamento).
                        .allowedHeaders("Content-Type", "Accept", "X-API-Key", "X-Idempotency-Key", "X-API-Version")
                        // Cabeçalhos que o JavaScript do navegador pode LER na resposta.
                        // Sem isto, o front-end não enxergaria o Location nem o Retry-After do 429.
                        .exposedHeaders("Location", "Retry-After", "X-RateLimit-Limit", "X-RateLimit-Remaining")
                        // A API não usa cookies, então não precisa de credenciais.
                        // (Credenciais + origem "*" seria uma combinação insegura e o Spring nem aceita.)
                        .allowCredentials(false)
                        .maxAge(3600); // O navegador guarda a resposta do preflight por 1 hora
            }
        };
    }
}
