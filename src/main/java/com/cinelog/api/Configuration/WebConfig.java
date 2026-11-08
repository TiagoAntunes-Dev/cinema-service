package com.cinelog.api.Configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig: configurações do Spring MVC da API (versionamento e interceptors).
 *
 * VERSIONAMENTO VIA HEADER
 * A versão da API NÃO fica na URL: as rotas são sempre /api/users, /api/movies etc., sem número de versão.
 * O cliente informa a versão no cabeçalho "X-API-Version" de cada requisição:
 * - Sem o cabeçalho: vale a versão padrão (DEFAULT_API_VERSION, a versão 1).
 * - Versão que não existe (ex.: 99) ou em formato inválido (ex.: abc): o Spring recusa com erro 400,
 *   e o GlobalExceptionHandler devolve a mensagem no formato APIError.
 * Quem lê e valida o cabeçalho é o próprio Spring (suporte nativo a versionamento do Spring Framework 7).
 * Os endpoints que não declaram "version" atendem todas as versões suportadas.
 *
 * O QUE CADA VERSÃO OFERECE
 * - Versão 1 (padrão): a API simples. Aberta, sem chave de API, sem limite de requisições e sem idempotência.
 * - Versão 2 (FIRST_SECURED_VERSION em diante): a API completa. Exige chave de API (X-API-Key), limita as
 *   requisições por chave (rate limit, erro 429), aceita idempotência nos POST (X-Idempotency-Key)
 *   e traz o novo formato do GET /movies/{id}.
 * Itens que valem para as duas versões: CORS, validações e tratamento de erros (GlobalExceptionHandler).
 *
 * COMO CRIAR UMA NOVA VERSÃO DE UM ENDPOINT (convenção do projeto):
 * - a versão antiga fica "congelada":      @GetMapping(value = "/movies/{id}", version = "1")
 * - a versão nova vale dela em diante ("+"): @GetMapping(value = "/movies/{id}", version = "2+")
 * - inclua o número em SUPPORTED_API_VERSIONS e crie o grupo dele em OpenApiVersionGroups (Swagger).
 * Exemplo pronto no projeto: MovieController.getMovie (v1) e getMovieV2 (v2).
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
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader(API_VERSION_HEADER)   // lê a versão do cabeçalho X-API-Version
                .setDefaultVersion(DEFAULT_API_VERSION)   // sem o cabeçalho, usa a versão 1
                .addSupportedVersions(SUPPORTED_API_VERSIONS); // só aceita as versões listadas
    }
}
