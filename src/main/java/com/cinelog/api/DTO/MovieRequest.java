package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * MovieRequest: DTO de ENTRADA para criar ou atualizar um filme.
 * (transportar dados com segurança)
 */
@Schema(description = "Dados necessários para a criação ou atualização de um filme")
public record MovieRequest(

        @NotBlank(message = "O título não pode estar em branco")
        @Size(min = 2, max = 150, message = "O título deve ter no 2 e 150 caracteres")
        @Schema(
                description = "Título do filme",
                example = "Interestelar",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String titulo,

        @NotNull(message = "O ano de lançamento é obrigatório")
        @Min(value = 1888, message = "O ano de lançamento deve ser a partir de 1888")
        @Max(value = 2100, message = "O ano de lançamento deve ser no máximo 2100")
        @Schema(
                description = "Ano de lançamento (entre 1888 e 2100)",
                example = "2014",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Integer anoLancamento,

        @NotNull(message = "A duração é obrigatória")
        @Positive(message = "A duração deve ser maior que zero")
        @Schema(
                description = "Duração do filme em minutos",
                example = "169",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Integer duracao,

        @NotBlank(message = "A classificação indicativa é obrigatória")
        @Pattern(
                regexp = "^(L|10|12|14|16|18)$",
                message = "A classificação indicativa deve ser L, 10, 12, 14, 16 ou 18"
        )
        @Schema(
                description = "Classificação indicativa (L, 10, 12, 14, 16 ou 18)",
                example = "10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String classificacaoIndicativa,

        @Schema(
                description = "IDs dos gêneros do filme (opcional). Os gêneros precisam já estar cadastrados",
                example = "[1, 2]"
        )

        // Filme pode ser criado sem Genre - Embora Incomum.
        Set<Long> genreIds
) {
}