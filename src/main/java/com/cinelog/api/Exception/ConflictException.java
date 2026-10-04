package com.cinelog.api.Exception; // Pacote dedicado a gerenciar os erros da aplicação

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ConflictException: exceção customizada para quando o pedido do cliente conflita com o que já existe
 * no banco (por exemplo, um nome repetido, ou apagar algo que ainda está em uso).
 * É igual à ResourceNotFoundException, mas devolve o erro 409 (Conflict) em vez do 404.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    // Construtor que repassa a mensagem de erro para a superclasse RuntimeException
    public ConflictException(String message) {
        super(message);
    }
}