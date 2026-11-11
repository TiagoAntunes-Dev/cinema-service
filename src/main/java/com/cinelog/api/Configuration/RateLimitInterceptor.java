package com.cinelog.api.Configuration;

import com.cinelog.api.Exception.TooManyRequestsException;
import com.cinelog.api.Service.RateLimitService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RateLimitInterceptor: "porteiro" que roda ANTES de cada controller (rotas /api/**, ver WebConfig).
 *
 * A versão da API decide QUEM é o cliente que está sendo limitado:
 * - Versão 2: a chave de API (validada antes pelo ApiKeyInterceptor): cada chave tem o seu balde.
 * - Versão 1: o endereço IP de quem chama, porque a v1 é aberta e não tem chave.
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
 *
 * Qual é o IP do cliente?
 * Em produção a aplicação fica atrás de proxies (no Render, atrás do Cloudflare), então request.getRemoteAddr()
 * devolveria o IP do proxy e TODOS os clientes dividiriam o mesmo balde. Por isso o IP é lido do cabeçalho
 * configurado em rate-limit.client-ip-header (padrão: CF-Connecting-IP, que o Cloudflare preenche).
 * Se o cabeçalho não vier (ex.: rodando localmente), usa o IP da conexão.
 * ATENÇÃO: só configure um cabeçalho em que você CONFIA (que o proxy sobrescreve). Se a aplicação ficar exposta
 * sem esse proxy, o cliente poderia inventar o cabeçalho e escapar do limite. Para desligar, deixe a propriedade vazia.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Um IP (inclusive IPv6) tem no máximo 45 caracteres. Valores maiores são lixo e são ignorados.
    private static final int MAX_IP_LENGTH = 45;

    private final RateLimitService rateLimitService;
    private final String clientIpHeader;

    public RateLimitInterceptor(RateLimitService rateLimitService,
                                @Value("${rate-limit.client-ip-header:CF-Connecting-IP}") String clientIpHeader) {
        this.rateLimitService = rateLimitService;
        this.clientIpHeader = clientIpHeader;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS é o "preflight" do CORS: o navegador pergunta se pode fazer a requisição real.
        // Se ele gastasse token, cada chamada do navegador custaria 2 tokens em vez de 1.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        ConsumptionProbe probe;
        long limit;

        Object apiKey = request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);
        if (ApiVersionSupport.isSecuredVersion(request) && apiKey != null) {
            // Versão 2: o cliente é identificado pela chave de API.
            probe = rateLimitService.tryConsumeByApiKey(apiKey.toString());
            limit = rateLimitService.getApiKeyCapacity();
        } else {
            // Versão 1 (aberta): o cliente é identificado pelo IP.
            probe = rateLimitService.tryConsumeByIp(resolveClientIp(request));
            limit = rateLimitService.getIpCapacity();
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));

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

    // Descobre o IP do cliente: cabeçalho de confiança (se configurado e presente) ou o IP da conexão.
    private String resolveClientIp(HttpServletRequest request) {
        if (clientIpHeader != null && !clientIpHeader.isBlank()) {
            String value = request.getHeader(clientIpHeader.trim());
            if (value != null && !value.isBlank()) {
                String first = value.split(",")[0].trim(); // cabeçalhos como X-Forwarded-For podem trazer uma lista
                if (!first.isEmpty() && first.length() <= MAX_IP_LENGTH) {
                    return first;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
