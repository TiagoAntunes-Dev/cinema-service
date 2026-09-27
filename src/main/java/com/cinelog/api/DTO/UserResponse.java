package com.cinelog.api.DTO; // Define o pacote onde ficam os modelos de transferência de dados (DTOs)

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * UserResponse: É o DTO de SAÍDA.
 * É usado para devolver dados ao cliente. Evitamos devolver a entidade 'User' direta
 * para garantir que dados sensíveis do banco (se houvessem, como senhas) nunca vazem na API.
 */
@Schema(description = "Representação dos dados do usuário retornados com sucesso pela API")
public record UserResponse(

        @Schema(description = "Identificador único gerado pelo banco de dados", example = "1")
        long id,

        @Schema(description = "Nome do usuário", example = "Tiago Antunes")
        String name,

        @Schema(description = "E-mail do usuário", example = "tiagoantunes1974@example.com")
        String email
) {
}