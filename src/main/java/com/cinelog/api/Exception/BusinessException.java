package com.cinelog.api.Exception;

/**
 * BusinessException: lançada quando uma regra de negócio é violada
 * (por exemplo, tentar trocar o dono de uma watchlist).
 * Deve ser capturada pelo @RestControllerAdvice e traduzida em HTTP 400 (ou 422).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}