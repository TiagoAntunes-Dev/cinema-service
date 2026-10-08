package com.cinelog.api.DTO; // Define o pacote onde ficam os modelos de transferência de dados (DTOs)

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * GenreRequest: DTO de ENTRADA recebe o JSON do cliente para criar ou atualizar um gênero.
 * (transportar dados com segurança)
 */
@Schema(description = "Dados necessários para a criação ou atualização de um gênero")
public record GenreRequest(

        @NotBlank(message = "O nome do gênero não pode estar em branco")
        @Size(min = 2 ,max = 50, message = "O nome do gênero deve ter entre 2 e 50 caracteres")
        @Schema(
                description = "Nome do gênero (não pode repetir)",
                example = "Ficção Científica",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String nome
) {
}
