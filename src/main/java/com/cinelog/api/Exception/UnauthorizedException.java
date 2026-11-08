package com.cinelog.api.Exception; // Pacote dedicado a gerenciar os erros da aplicação

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * UnauthorizedException: lançada pelo ApiKeyInterceptor quando a chave de API (cabeçalho X-API-Key)
 * não foi enviada ou não é válida. É igual às outras exceções do pacote, mas devolve o erro 401 (Unauthorized).
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}