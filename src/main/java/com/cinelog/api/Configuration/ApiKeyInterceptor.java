package com.cinelog.api.Configuration;

import com.cinelog.api.Exception.UnauthorizedException;
import com.cinelog.api.Service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ApiKeyInterceptor: "porteiro" da AUTENTICAÇÃO POR CHAVE DE API. Roda antes de cada controller (rotas /api/**).
 *
 * Como funciona:
 * - Versão 1: a API é aberta, a requisição passa direto (a chave não é pedida).
 * - Versão 2: o cliente precisa enviar o cabeçalho X-API-Key com uma chave cadastrada (ApiKeyService).
 *     - sem o cabeçalho ou com chave inválida: erro 401 (Unauthorized), no formato APIError;
 *     - com chave válida: a chave fica guardada na requisição, e o rate limit usa ela para identificar o cliente.
 * - Requisições OPTIONS (preflight do CORS) passam sempre: o navegador não envia cabeçalhos
 *   personalizados nelas, então exigir a chave aqui travaria qualquer front-end.
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    // Nome do "campo" onde guardamos a chave validada, para o RateLimitInterceptor ler depois.
    public static final String API_KEY_ATTRIBUTE = "cinelog.apiKey";

    private final ApiKeyService apiKeyService;

    public ApiKeyInterceptor(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Versão 1: sem autenticação.
        if (!ApiVersionSupport.isSecuredVersion(request)) {
            return true;
        }

        String apiKey = request.getHeader(WebConfig.API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("Chave de API ausente. Informe o cabeçalho "
                    + WebConfig.API_KEY_HEADER + " (obrigatório na versão 2 da API).");
        }
        if (!apiKeyService.isValid(apiKey)) {
            throw new UnauthorizedException("Chave de API inválida.");
        }

        request.setAttribute(API_KEY_ATTRIBUTE, apiKey.trim());
        return true;
    }
}
