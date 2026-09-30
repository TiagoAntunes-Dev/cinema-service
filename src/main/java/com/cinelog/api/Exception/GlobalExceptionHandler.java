package com.cinelog.api.Exception;

import com.cinelog.api.DTO.APIError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     * @ExceptionHandler: Diz qual tipo exato de exceção este método vai capturar.
     * Aqui ele captura a "MethodArgumentNotValidException", que é o erro disparado
     * quando as validações @NotBlank e @Email falham no Controller.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<APIError> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException exception,
            HttpServletRequest request) { // Injeta a requisição original para descobrirmos qual rota deu erro

        HttpStatus status = HttpStatus.BAD_REQUEST; // Erros de validação do cliente são sempre 400 (Bad Request)

        // Navega por dentro do objeto de exceção do Spring para extrair a mensagem customizada que colocamos no @NotBlank
        String errorMessage = exception.getBindingResult().getFieldError().getDefaultMessage();

        // Monta o nosso DTO de erro padrão
        APIError apiError = new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), // Pega a hora atual sem milissegundos
                status.value(), // Ex: 400
                status.name(), // Ex: "BAD_REQUEST"
                errorMessage, // Ex: "O nome não pode estar em branco"
                request.getRequestURI() // Ex: "/api/v1/users"
        );

        return ResponseEntity.status(status).body(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        APIError error = new APIError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(), // Status 400 (Requisição Inválida)
                "BAD_REQUEST",
                ex.getMessage(), // Aqui vai vir a mensagem: "Já existe um usuário cadastrado com o e-mail..."
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Tratamento de exceções genéricas. Captura o super tipo "Exception".
     * Serve como última linha de defesa para erros que não previmos (como falha no banco),
     * retornando 500 (Internal Server Error) para não vazar a "stack trace" feia do Java para o cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIError> handleGenericException(Exception exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        APIError apiError = new APIError(
                LocalDateTime.now(),
                status.value(),
                status.name(),
                "Ocorreu um erro interno no servidor.",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(apiError);
    }

    /**
     * Captura a nossa exceção customizada de quando o Service busca um ID que não existe.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(ResourceNotFoundException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND; // 404 Not Found

        APIError apiError = new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.name(),
                exception.getMessage(), // Mensagem que enviamos lá do UserService ("Usuário não encontrado com o ID: X")
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(apiError);
    }
}
