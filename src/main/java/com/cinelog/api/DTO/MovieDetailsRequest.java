package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 DTO de ENTRADA para criar ou atualizar os detalhes de um filme.
 (transportar dados com segurança)
 */
@Schema(description = "Dados necessários para a criação ou atualização dos detalhes de um filme")
public record MovieDetailsRequest(

        @NotNull(message = "O id do filme é obrigatório")
        @Positive(message = "O id do filme deve ser maior que zero")
        @Schema(
                description = "Id do filme dono destes detalhes (cada filme tem no máximo um registro de detalhes)",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long movieId,

        @Size(max = 4000, message = "A sinopse deve ter no máximo 4000 caracteres")
        @Schema(
                description = "Sinopse completa do filme",
                example = "Um grupo de astronautas viaja por um buraco de minhoca em busca de um novo lar para a humanidade."
        )
        String sinopseLonga,

        @PositiveOrZero(message = "O orçamento não pode ser negativo")
        @Schema(description = "Orçamento do filme", example = "165000000.00")
        BigDecimal orcamento,

        @Size(max = 60, message = "O país de origem deve ter no máximo 60 caracteres")
        @Schema(description = "País de origem do filme", example = "Estados Unidos")
        String paisOrigem,

        @Size(max = 40, message = "O idioma original deve ter no máximo 40 caracteres")
        @Schema(description = "Idioma original do filme", example = "Inglês")
        String idiomaOriginal,

        @Size(max = 2000, message = "As notas de produção devem ter no máximo 2000 caracteres")
        @Schema(description = "Observações sobre a produção do filme", example = "Parte das cenas foi filmada na Islândia.")
        String notasProducao
) {
}