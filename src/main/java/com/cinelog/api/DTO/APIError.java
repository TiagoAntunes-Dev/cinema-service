package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * APIError: Padroniza a resposta de erro da nossa API.
 * Se der erro, o cliente frontend sempre vai saber que receberá um JSON com timestamp, status, mensagem e o caminho(path).
 */
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

        @Schema(description = "Caminho da rota onde o erro foi disparado", example = "/api/v1/users/99")
        String path

) {
}