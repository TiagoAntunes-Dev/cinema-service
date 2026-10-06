package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Record: Um recurso moderno do Java que cria classes imutáveis automaticamente.
 * Ele já gera por debaixo dos panos os métodos getters, construtor completo, equals, hashCode e toString.
 *
 * UserRequest: É o DTO de ENTRADA. Define exatamente os campos que a API aceita receber (JSON do Postman/Swagger).
 *
 * DTO é a intenção/papel (transportar dados com segurança).
 */
@Schema(description = "Dados necessários para a criação ou atualização de um usuário na Fase 1")
public record UserRequest(

        @NotBlank(message = "O nome não pode estar em branco")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        @Schema(
                description = "Nome completo do usuário",
                example = "Tiago Antunes",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @NotBlank(message = "O email não pode estar em branco")
        @Email(message = "O email deve ser um endereço válido")
        @Size(max = 150, message = "O email deve ter no máximo 150 caracteres")
        @Schema(
                description = "E-mail único do usuário",
                example = "tiagoantunes1974@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email
) {
}