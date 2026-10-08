package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * MovieResponse: DTO de SAÍDA com os dados de um filme.
 * (transportar dados com segurança)
 */
@Schema(description = "Representação dos dados do filme retornados com sucesso pela API")
public record MovieResponse(

        @Schema(
                description = "Identificador único gerado pelo banco de dados",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        long id,

        @Schema(description = "Título do filme", example = "Interestelar")
        String titulo,

        @Schema(description = "Ano de lançamento", example = "2014")
        int anoLancamento,

        @Schema(description = "Duração do filme em minutos", example = "169")
        int duracao,

        @Schema(description = "Classificação indicativa", example = "10")
        String classificacaoIndicativa,

        @Schema(description = "Gêneros associados ao filme")

        // Devolve o Objeto Completo (ID, Nome)
        List<GenreResponse> generos
) {
}