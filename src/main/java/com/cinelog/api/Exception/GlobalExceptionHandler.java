package com.cinelog.api.Exception;

import com.cinelog.api.DTO.APIError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @RestControllerAdvice: É um interceptador global. Fica "escutando" a aplicação inteira.
 * Se qualquer Controller lançar uma exceção, essa classe captura o erro antes de ele chegar no cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Erros de validação do Bean Validation (@NotBlank, @Email, @Size...) nos DTOs.
     * Retorna 400 (Bad Request) com o formato APIError.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIError> handleValidationExceptions(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        // Pega a primeira mensagem de erro de campo. Se não houver field error, usa uma mensagem genérica.
        String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .findFirst()
                .orElse("Erro de validação");

        APIError apiError = new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.name(),
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(apiError);
    }

    /**
     * Captura a nossa exceção customizada de quando o Service busca um ID que não existe.
     * Retorna 404 (Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        APIError apiError = new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.name(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(apiError);
    }

    /**
     * Captura a nossa exceção customizada de regra de negócio violada
     * (ex.: tentar trocar o dono de uma watchlist).
     * Retorna 400 (Bad Request).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<APIError> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        APIError apiError = new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.name(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(apiError);
    }

    /**
     * Captura a nossa exceção customizada de conflito com o estado atual do banco
     * (ex.: nome de gênero duplicado, filme já na watchlist).
     * Retorna 409 (Conflict).
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<APIError> handleConflictException(
            ConflictException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.CONFLICT;

        APIError apiError = new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.name(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(apiError);
    }

    /**
     * Tratamento de exceções genéricas. Captura o super tipo "Exception".
     * Serve como última linha de defesa para erros que não previmos (como falha no banco),
     * retornando 500 (Internal Server Error) para não vazar a "stack trace" feia do Java para o cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIError> handleGenericException(
            Exception exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        APIError apiError = new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.name(),
                "Ocorreu um erro interno no servidor.",
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(apiError);
    }
}
