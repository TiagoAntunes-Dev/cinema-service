package com.cinelog.api.Configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * WebConfig: configurações do Spring MVC da API (versionamento e interceptors).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Nome do cabeçalho HTTP que carrega a versão da API.
    public static final String API_VERSION_HEADER = "X-API-Version";

    // Versão usada quando o cliente não envia o cabeçalho.
    public static final String DEFAULT_API_VERSION = "1";

    // Versões que a API aceita. Ao lançar a versão 3, inclua "3" aqui.
    public static final String[] SUPPORTED_API_VERSIONS = {"1", "2"};

    // A partir desta versão a API exige chave de API, aplica rate limit e aceita idempotência.
    public static final int FIRST_SECURED_VERSION = 2;

    // Cabeçalho que carrega a chave de API (versão 2).
    public static final String API_KEY_HEADER = "X-API-Key";

    // Cabeçalho de idempotência dos POST (versão 2).
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(ApiKeyInterceptor apiKeyInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.apiKeyInterceptor = apiKeyInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // A ORDEM IMPORTA: primeiro confere a chave de API (401 se faltar ou for inválida);
        // só depois conta a requisição no rate limit, já sabendo de qual chave ela veio.
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(100); // Define o limite máximo de itens por página (ex: 100)
        resolvers.add(resolver);
    }
}