package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * GenreResponse: DTO de SAÍDA com os dados de um gênero.
 */
@Schema(description = "Representação dos dados do gênero retornados com sucesso pela API")
public record GenreResponse(

        @Schema(
                description = "Identificador único gerado pelo banco de dados",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        long id,

        @Schema(description = "Nome do gênero", example = "Ficção Científica")
        String nome
) {
}