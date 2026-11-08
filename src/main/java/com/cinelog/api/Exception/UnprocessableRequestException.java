package com.cinelog.api.Exception; // Pacote dedicado a gerenciar os erros da aplicação

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * UnprocessableRequestException: lançada quando a requisição é válida no formato, mas não pode ser processada.
 * Hoje é usada pelo IdempotencyService: o cliente repetiu um X-Idempotency-Key que já tinha sido usado
 * com um corpo de requisição DIFERENTE (a chave deve identificar sempre a mesma operação).
 * É igual às outras exceções do pacote, mas devolve o erro 422 (Unprocessable Entity).
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class UnprocessableRequestException extends RuntimeException {

    public UnprocessableRequestException(String message) {
        super(message);
    }
}
