package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * WatchListResponse: DTO de SAÍDA com os dados de uma watchlist.
 * (transportar dados com segurança)
 */
@Schema(description = "Representação dos dados da watchlist retornados com sucesso pela API")
public record WatchListResponse(

        @Schema(
                description = "Identificador único gerado pelo banco de dados",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        long id,

        @Schema(description = "Nome da watchlist", example = "Para ver no fim de semana")
        String nome,

        @Schema(description = "Descrição da watchlist", example = "Filmes de ficção científica que ainda não assisti")
        String descricao,

        @Schema(
                description = "Id do usuário dono da watchlist",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        long userId
) {
}