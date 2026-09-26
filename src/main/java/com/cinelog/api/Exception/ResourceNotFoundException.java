package com.cinelog.api.Exception; // Pacote dedicado a gerenciar os erros da aplicação

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ResourceNotFoundException: Uma exceção customizada (criada por nós).
 * @ResponseStatus(HttpStatus.NOT_FOUND): Avisa ao Spring que, caso essa exceção não seja
 * tratada por ninguém, ele deve retornar um erro 404 por padrão.
 *
 * extends RuntimeException: Significa que é uma exceção que ocorre em tempo de execução
 * (não obriga o uso de try-catch em todos os lugares do código).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    // Construtor que repassa a mensagem de erro para a superclasse RuntimeException
    public ResourceNotFoundException(String message) {
        super(message);
    }
}