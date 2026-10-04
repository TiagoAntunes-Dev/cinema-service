package com.cinelog.api.DTO;

import com.cinelog.api.Entity.WatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * WatchlistItemResponse: DTO de SAÍDA com os dados de um item da watchlist.
 * Além do id do filme, devolve o título (movieTitulo) para o cliente não precisar fazer outra consulta.
 */
@Schema(description = "Representação dos dados do item da watchlist retornados com sucesso pela API")
public record WatchListItemResponse(

        @Schema(description = "Identificador único gerado pelo banco de dados", example = "1")
        long id,

        @Schema(description = "Id da watchlist à qual o item pertence", example = "1")
        long watchlistId,

        @Schema(description = "Id do filme", example = "1")
        long movieId,

        @Schema(description = "Título do filme", example = "Interestelar")
        String movieTitulo,

        @Schema(description = "Situação do filme na lista", example = "QUERO_VER")
        WatchStatus status,

        @Schema(description = "Nota de 1 a 10 (pode ser nula)", example = "9")
        Integer nota
) {
}