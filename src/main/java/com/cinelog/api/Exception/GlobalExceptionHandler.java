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
import org.springframework.web.accept.InvalidApiVersionException;
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
 * @RestControllerAdvice: É um interceptador global. Fica "escutando" a aplicação inteira.
 * Se qualquer Controller (ou o RateLimitInterceptor) lançar uma exceção, essa classe captura o erro
 * antes de ele chegar no cliente e devolve sempre o mesmo formato: APIError.
 *
 * Resumo dos status devolvidos:
 *  400 -> dados inválidos (validação, JSON quebrado, parâmetro com tipo errado ou ausente, versão da API inexistente, regra de negócio)
 *  401 -> chave de API ausente ou inválida (versão 2)
 *  404 -> recurso ou rota não encontrados
 *  405 -> método HTTP não aceito pela rota
 *  409 -> conflito (duplicado ou ainda em uso)
 *  415 -> corpo enviado em formato não suportado
 *  422 -> requisição entendida, mas não processável (chave de idempotência reutilizada com outro corpo)
 *  429 -> limite de requisições excedido
 *  500 -> erro inesperado do servidor
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Logger: registra no console os erros inesperados, para podermos descobrir a causa depois.
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =====================================================================================
    // 400 - BAD REQUEST: o cliente enviou algo inválido
    // =====================================================================================

    /**
     * Erros de validação do Bean Validation (@NotBlank, @Email, @Size...) nos DTOs.
     * Devolve TODOS os campos inválidos de uma vez, na lista "errors" (ex.: "name: O nome não pode estar em branco"),
     * para o cliente corrigir tudo de uma só vez em vez de descobrir um erro por tentativa.
     */
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

    /**
     * Validação disparada fora do @RequestBody (por exemplo, ao salvar uma entidade com anotações de validação).
     */
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

    /**
     * Corpo da requisição ausente, JSON mal formado (faltou uma vírgula, aspas...) ou valor que não
     * cabe no campo (texto num campo numérico, status fora do enum...). Antes isso virava erro 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIError> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        String message = "Corpo da requisição ausente ou inválido. Verifique se o JSON está bem formado "
                + "e se os números e os valores fixos (como o status) estão corretos.";

        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Parâmetro da URL com o tipo errado. Exemplos: GET /users/abc (o id deve ser número)
     * ou ?status=ASSISTIDO (valor que não existe no enum WatchStatus).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        String message = "O parâmetro '" + exception.getName() + "' recebeu o valor '" + exception.getValue()
                + "', mas era esperado " + describeExpectedType(exception.getRequiredType()) + ".";

        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Parâmetro obrigatório da URL que não foi enviado. Exemplo: GET /movies/search sem o ?titulo=.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIError> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {

        String message = "O parâmetro '" + exception.getParameterName() + "' é obrigatório e não foi informado.";

        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Versão da API pedida no cabeçalho X-API-Version que não existe (ex.: "99") ou está em formato inválido (ex.: "abc").
     * Quem lê e valida o cabeçalho é o próprio Spring (configurado no WebConfig); aqui só traduzimos o erro
     * para o nosso formato. Sem o cabeçalho, a versão padrão é usada e nenhum erro acontece.
     */
    @ExceptionHandler(InvalidApiVersionException.class)
    public ResponseEntity<APIError> handleInvalidApiVersion(
            InvalidApiVersionException exception,
            HttpServletRequest request) {

        String message = "Versão da API não suportada: '" + request.getHeader(WebConfig.API_VERSION_HEADER) + "'. "
                + "Informe no cabeçalho " + WebConfig.API_VERSION_HEADER
                + " uma das versões disponíveis: " + String.join(", ", WebConfig.SUPPORTED_API_VERSIONS) + ".";

        return respond(HttpStatus.BAD_REQUEST, message, request);
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

        return respond(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    // =====================================================================================
    // 401 - UNAUTHORIZED (Chave de API)
    // =====================================================================================

    /**
     * Lançada pelo ApiKeyInterceptor quando a chave de API (X-API-Key) não foi enviada ou é inválida.
     * Além do 401, envia o cabeçalho WWW-Authenticate, que diz ao cliente qual tipo de credencial a API espera.
     */
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

    /**
     * Captura a nossa exceção customizada de quando o Service busca um ID que não existe.
     * Retorna 404 (Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        return respond(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    // =====================================================================================
    // 409 - CONFLICT
    // =====================================================================================

    /**
     * Captura a nossa exceção customizada de conflito com o estado atual do banco
     * (ex.: nome de gênero duplicado, filme já na watchlist).
     * Retorna 409 (Conflict).
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<APIError> handleConflictException(
            ConflictException exception,
            HttpServletRequest request) {

        return respond(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    /**
     * Rede de segurança do banco: se uma regra de integridade (UNIQUE, chave estrangeira) for violada
     * e o Service não tiver checado antes, o erro vira 409 em vez de 500.
     */
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
    // 422 - UNPROCESSABLE ENTITY (Idempotência)
    // =====================================================================================

    /**
     * Lançada pelo IdempotencyService quando o mesmo X-Idempotency-Key é reutilizado com um corpo diferente.
     * A chave deve identificar sempre a mesma operação; reaproveitá-la para outra é um uso indevido.
     * Retorna 422 (Unprocessable Entity).
     */
    @ExceptionHandler(UnprocessableRequestException.class)
    public ResponseEntity<APIError> handleUnprocessableRequest(
            UnprocessableRequestException exception,
            HttpServletRequest request) {

        return respond(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    // =====================================================================================
    // 429 - TOO MANY REQUESTS (Rate Limit)
    // =====================================================================================

    /**
     * Lançada pelo RateLimitInterceptor quando o balde de tokens do cliente esvazia.
     * Além do 429, envia o cabeçalho Retry-After (em segundos) dizendo quando tentar de novo.
     */
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
    // Última linha de defesa
    // =====================================================================================

    /**
     * Tratamento de exceções genéricas. Captura o super tipo "Exception".
     *
     * 1) Erros HTTP padrão do próprio Spring (rota inexistente = 404, método errado = 405,
     *    Content-Type não suportado = 415...) implementam a interface ErrorResponse, que já traz o status
     *    correto. Sem este tratamento, todos eles virariam 500, o que seria enganoso.
     * 2) O que sobrar é erro que não previmos (como falha no banco): retorna 500 (Internal Server Error)
     *    sem vazar a "stack trace" feia do Java para o cliente, mas registrando o erro completo no log.
     */
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

                // getHeaders() traz cabeçalhos úteis do próprio erro (ex.: "Allow" com os métodos aceitos no 405).
                return ResponseEntity.status(status)
                        .headers(errorResponse.getHeaders())
                        .body(createError(status, message, request, List.of()));
            }
        }

        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), exception);

        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor.", request);
    }

    // =====================================================================================
    // Métodos auxiliares (evitam repetir o mesmo código em todos os handlers)
    // =====================================================================================

    // Monta o corpo padrão do erro (APIError).
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

    // Erro simples, sem lista de detalhes.
    private ResponseEntity<APIError> respond(HttpStatus status, String message, HttpServletRequest request) {
        return respond(status, message, request, List.of());
    }

    // Erro com lista de detalhes (usado na validação).
    private ResponseEntity<APIError> respond(HttpStatus status, String message, HttpServletRequest request, List<String> errors) {
        return ResponseEntity.status(status).body(createError(status, message, request, errors));
    }

    // Texto amigável do que era esperado num parâmetro, usado na mensagem de tipo inválido.
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