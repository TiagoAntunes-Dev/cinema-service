package com.cinelog.api.Exception; // Pacote dedicado a gerenciar os erros da aplicação

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * TooManyRequestsException: lançada pelo RateLimitInterceptor quando o cliente gasta todos os
 * tokens do seu balde (limite de requisições excedido).
 * É igual às outras exceções do pacote, mas devolve o erro 429 (Too Many Requests)
 * e carrega o tempo de espera, que o GlobalExceptionHandler coloca no cabeçalho Retry-After.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super("Limite de requisições excedido. Tente novamente em " + retryAfterSeconds + " segundo(s).");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
