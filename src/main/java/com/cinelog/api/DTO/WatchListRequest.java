package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * WatchListRequest: DTO de ENTRADA para criar ou atualizar uma watchlist.
 * (transportar dados com segurança)
 */
@Schema(description = "Dados necessários para a criação ou atualização de uma watchlist")
public record WatchListRequest(

        @NotBlank(message = "O nome da watchlist não pode estar em branco")
        @Size(min = 3, max = 100, message = "O nome da watchlist deve ter entre 3 e 100 caracteres")
        @Schema(
                description = "Nome da watchlist",
                example = "Para ver no fim de semana",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String nome,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        @Schema(
                description = "Descrição da watchlist (opcional)",
                example = "Filmes de ficção científica que ainda não assisti"
        )
        String descricao,

        @NotNull(message = "O id do usuário é obrigatório")
        @Positive(message = "O id do usuário deve ser maior que zero")
        @Schema(
                description = "Id do usuário dono da watchlist",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long userId
) {
}
