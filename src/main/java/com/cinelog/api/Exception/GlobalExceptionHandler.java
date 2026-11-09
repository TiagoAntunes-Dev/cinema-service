package com.cinelog.api.Exception;

import com.cinelog.api.Configuration.WebConfig;
import com.cinelog.api.DTO.APIError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * GlobalExceptionHandler: interceptador global de exceções da API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =====================================================================================
    // 400 - BAD REQUEST
    // =====================================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIError> handleValidationExceptions(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<String> errors = exception.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .sorted()
                .toList();

        String message = "Erro de validação: " + errors.size() + " problema(s) encontrado(s). Veja os detalhes em 'errors'.";

        return respond(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<APIError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {

        List<String> errors = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .toList();

        String message = "Erro de validação: " + errors.size() + " problema(s) encontrado(s). Veja os detalhes em 'errors'.";

        return respond(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIError> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        String message = "Corpo da requisição ausente ou inválido. Verifique se o JSON está bem formado "
                + "e se os números e os valores fixos (como o status) estão corretos.";

        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        String message = "O parâmetro '" + exception.getName() + "' recebeu o valor '" + exception.getValue()
                + "', mas era esperado " + describeExpectedType(exception.getRequiredType()) + ".";

        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIError> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {

        String message = "O parâmetro '" + exception.getParameterName() + "' é obrigatório e não foi informado.";

        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(InvalidApiVersionException.class)
    public ResponseEntity<APIError> handleInvalidApiVersion(
            InvalidApiVersionException exception,
            HttpServletRequest request) {

        String message = "Versão da API não suportada: '" + request.getHeader(WebConfig.API_VERSION_HEADER) + "'. "
                + "Informe no cabeçalho " + WebConfig.API_VERSION_HEADER
                + " uma das versões disponíveis: " + String.join(", ", WebConfig.SUPPORTED_API_VERSIONS) + ".";

        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<APIError> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {

        return respond(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    // =====================================================================================
    // 401 - UNAUTHORIZED
    // =====================================================================================

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<APIError> handleUnauthorized(
            UnauthorizedException exception,
            HttpServletRequest request) {

        APIError apiError = createError(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, List.of());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey realm=\"Cinelog API\"")
                .body(apiError);
    }

    // =====================================================================================
    // 404 - NOT FOUND
    // =====================================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        return respond(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    // =====================================================================================
    // 409 - CONFLICT
    // =====================================================================================

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<APIError> handleConflictException(
            ConflictException exception,
            HttpServletRequest request) {

        return respond(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        log.warn("Violação de integridade em {}: {}", request.getRequestURI(), exception.getMostSpecificCause().getMessage());

        String message = "A operação viola uma regra de integridade dos dados "
                + "(registro duplicado ou ainda em uso por outro registro).";

        return respond(HttpStatus.CONFLICT, message, request);
    }

    // =====================================================================================
    // 422 - UNPROCESSABLE ENTITY
    // =====================================================================================

    @ExceptionHandler(UnprocessableRequestException.class)
    public ResponseEntity<APIError> handleUnprocessableRequest(
            UnprocessableRequestException exception,
            HttpServletRequest request) {

        return respond(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    // =====================================================================================
    // 429 - TOO MANY REQUESTS
    // =====================================================================================

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<APIError> handleTooManyRequests(
            TooManyRequestsException exception,
            HttpServletRequest request) {

        APIError apiError = createError(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request, List.of());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .body(apiError);
    }

    // =====================================================================================
    // Exceções genéricas
    // =====================================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIError> handleGenericException(
            Exception exception,
            HttpServletRequest request) {

        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());

            if (status != null) {
                String message = switch (status) {
                    case NOT_FOUND -> "Rota não encontrada: " + request.getRequestURI();
                    case METHOD_NOT_ALLOWED -> "O método " + request.getMethod()
                            + " não é permitido para a rota " + request.getRequestURI() + ".";
                    case UNSUPPORTED_MEDIA_TYPE -> "Formato de conteúdo não suportado. "
                            + "Envie o corpo da requisição como application/json.";
                    case NOT_ACCEPTABLE -> "Formato de resposta não disponível. Use o cabeçalho Accept: application/json.";
                    default -> errorResponse.getBody().getDetail() != null
                            ? errorResponse.getBody().getDetail()
                            : status.name();
                };

                return ResponseEntity.status(status)
                        .headers(errorResponse.getHeaders())
                        .body(createError(status, message, request, List.of()));
            }
        }

        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), exception);

        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor.", request);
    }

    // Auxiliares
    private APIError createError(HttpStatus status, String message, HttpServletRequest request, List<String> errors) {
        return new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.name(),
                message,
                request.getRequestURI(),
                errors
        );
    }

    private ResponseEntity<APIError> respond(HttpStatus status, String message, HttpServletRequest request) {
        return respond(status, message, request, List.of());
    }

    private ResponseEntity<APIError> respond(HttpStatus status, String message, HttpServletRequest request, List<String> errors) {
        return ResponseEntity.status(status).body(createError(status, message, request, errors));
    }

    private String describeExpectedType(Class<?> type) {
        if (type == null) {
            return "um valor válido";
        }
        if (type.isEnum()) {
            return "um dos valores " + Arrays.toString(type.getEnumConstants());
        }
        if (type == long.class || type == Long.class || type == int.class || type == Integer.class) {
            return "um número inteiro";
        }
        return "um valor do tipo " + type.getSimpleName();
    }
}