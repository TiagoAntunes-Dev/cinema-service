package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * MovieDetailsResponse: DTO de SAÍDA com os detalhes de um filme.
 */
@Schema(description = "Representação dos detalhes do filme retornados com sucesso pela API")
public record MovieDetailsResponse(

        @Schema(
                description = "Identificador único gerado pelo banco de dados",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        long id,

        @Schema(
                description = "Id do filme dono destes detalhes",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        long movieId,

        @Schema(
                description = "Sinopse completa do filme",
                example = "Um grupo de astronautas viaja por um buraco de minhoca em busca de um novo lar para a humanidade."
        )
        String sinopseLonga,

        @Schema(description = "Orçamento do filme", example = "165000000.00")
        BigDecimal orcamento,

        @Schema(description = "País de origem do filme", example = "Estados Unidos")
        String paisOrigem,

        @Schema(description = "Idioma original do filme", example = "Inglês")
        String idiomaOriginal,

        @Schema(description = "Observações sobre a produção do filme", example = "Parte das cenas foi filmada na Islândia.")
        String notasProducao
) {
}