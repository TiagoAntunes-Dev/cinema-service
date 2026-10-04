package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * MovieDetailsPatchRequest: DTO de ENTRADA para ATUALIZAÇÃO PARCIAL (PATCH) dos detalhes de um filme.
 * Todos os campos são OPCIONAIS: só os que vierem no JSON serão atualizados.
 * O movieId NÃO está aqui, pois o vínculo com o filme é imutável.
 */
@Schema(description = "Dados para atualização parcial (PATCH) dos detalhes de um filme. Todos os campos são opcionais.")
public record MovieDetailsPatchRequest(

        @Size(max = 4000, message = "A sinopse deve ter no máximo 4000 caracteres")
        @Schema(description = "Sinopse completa do filme")
        String sinopseLonga,

        @PositiveOrZero(message = "O orçamento não pode ser negativo")
        @Schema(description = "Orçamento do filme")
        BigDecimal orcamento,

        @Size(max = 60, message = "O país de origem deve ter no máximo 60 caracteres")
        @Schema(description = "País de origem do filme")
        String paisOrigem,

        @Size(max = 40, message = "O idioma original deve ter no máximo 40 caracteres")
        @Schema(description = "Idioma original do filme")
        String idiomaOriginal,

        @Size(max = 2000, message = "As notas de produção devem ter no máximo 2000 caracteres")
        @Schema(description = "Observações sobre a produção do filme")
        String notasProducao
) {
}
