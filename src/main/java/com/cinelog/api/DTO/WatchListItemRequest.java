package com.cinelog.api.DTO;

import com.cinelog.api.Entity.WatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * WatchlistItemRequest: DTO de ENTRADA para adicionar ou atualizar um filme dentro de uma watchlist.
 * A watchlist e o filme chegam como ids; o Service busca cada um no banco.
 */
@Schema(description = "Dados necessários para a criação ou atualização de um item da watchlist")
public record WatchListItemRequest(

        @NotNull(message = "O id da watchlist é obrigatório")
        @Schema(
                description = "Id da watchlist que receberá o filme",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long watchlistId,

        @NotNull(message = "O id do filme é obrigatório")
        @Schema(
                description = "Id do filme a ser adicionado",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long movieId,

        // WatchStatus é o enum da entidade: só aceita QUERO_VER, VENDO ou VISTO
        @NotNull(message = "O status é obrigatório")
        @Schema(
                description = "Situação do filme na lista: QUERO_VER, VENDO ou VISTO",
                example = "QUERO_VER",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        WatchStatus status,

        // Opcional: sem nota, o valor fica null (o @Min e o @Max ignoram null)
        @Min(value = 1, message = "A nota deve ser no mínimo 1")
        @Max(value = 10, message = "A nota deve ser no máximo 10")
        @Schema(description = "Nota de 1 a 10 (opcional)", example = "9")
        Integer nota
) {
}
