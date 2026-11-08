package com.cinelog.api.Configuration;

import com.cinelog.api.Exception.TooManyRequestsException;
import com.cinelog.api.Service.RateLimitService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RateLimitInterceptor: "porteiro" que roda ANTES de cada controller (rotas /api/**, ver WebConfig).
 * O limite vale a partir da versão 2 da API; na versão 1 não há limite (nenhum cabeçalho é enviado).
 * O cliente é identificado pela chave de API (validada antes pelo ApiKeyInterceptor): cada chave tem o seu balde.
 *
 * A cada requisição ele tenta gastar 1 token do balde do cliente:
 * - Deu certo: informa o limite e quantos tokens restam nos cabeçalhos e deixa a requisição passar.
 * - Balde vazio: lança TooManyRequestsException. O GlobalExceptionHandler transforma isso em
 *   HTTP 429 com o cabeçalho Retry-After, no mesmo formato de erro (APIError) do resto da API.
 *
 * Cabeçalhos enviados em TODA resposta:
 *   X-RateLimit-Limit      -> tamanho máximo do balde
 *   X-RateLimit-Remaining  -> tokens que ainda restam (0 quando bloqueado)
 * Cabeçalho enviado apenas no 429 (pelo GlobalExceptionHandler):
 *   Retry-After            -> em quantos segundos o cliente pode tentar de novo
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS é o "preflight" do CORS: o navegador pergunta se pode fazer a requisição real.
        // Se ele gastasse token, cada chamada do navegador custaria 2 tokens em vez de 1.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Versão 1: sem rate limit.
        if (!ApiVersionSupport.isSecuredVersion(request)) {
            return true;
        }

        // Cliente identificado pela chave de API validada. Se por algum motivo não houver chave, cai para o IP.
        // (Atrás de um proxy/load balancer o IP seria o do proxy; o certo seria configurar
        // server.forward-headers-strategy, o que não é o caso local.)
        Object apiKey = request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);
        String clientKey = (apiKey != null) ? "key:" + apiKey : "ip:" + request.getRemoteAddr();

        ConsumptionProbe probe = rateLimitService.tryConsume(clientKey);

        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitService.getCapacity()));

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        // Balde vazio. Arredonda para CIMA (nunca 0 segundo, senão o cliente tentaria cedo demais).
        long nanosToWait = probe.getNanosToWaitForRefill();
        long retryAfterSeconds = Math.max(1, (nanosToWait + 999_999_999L) / 1_000_000_000L);

        response.setHeader("X-RateLimit-Remaining", "0");
        throw new TooManyRequestsException(retryAfterSeconds);
    }
}