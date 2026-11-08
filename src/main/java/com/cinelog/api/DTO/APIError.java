package com.cinelog.api.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * APIError: Padroniza a resposta de erro da nossa API.
 *
 * @JsonInclude(NON_EMPTY): campos vazios não aparecem no JSON. Assim, "errors" só surge
 * nos erros de validação (400), e os demais erros continuam com o mesmo formato de antes.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Estrutura padronizada para retorno de erros da API")
public record APIError(

        @Schema(description = "Momento exato em que o erro ocorreu", example = "2026-09-26T21:30:00")
        LocalDateTime timestamp,

        @Schema(description = "Código de status HTTP", example = "404")
        int status,

        @Schema(description = "Nome descritivo do status HTTP", example = "NOT_FOUND")
        String errorMessage,

        @Schema(description = "Mensagem explicativa detalhada do erro", example = "Usuário não encontrado com o ID: 99")
        String message,

        @Schema(description = "Caminho da rota onde o erro foi disparado", example = "/api/users/99")
        String path,

        @Schema(description = "Detalhes do erro, um item por problema. Aparece nos erros de validação (400), com o campo e o que está errado",
                example = "[\"email: O email deve ser um endereço válido\", \"name: O nome não pode estar em branco\"]")
        List<String> errors

) {

    // Construtor para os erros que não têm lista de detalhes: "errors" fica vazio e some do JSON.
    public APIError(LocalDateTime timestamp, int status, String errorMessage, String message, String path) {
        this(timestamp, status, errorMessage, message, path, List.of());
    }
}
