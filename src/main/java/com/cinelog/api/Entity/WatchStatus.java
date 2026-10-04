package com.cinelog.api.Entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * WatchStatus: ENUM (conjunto fixo de valores) que diz em que etapa um filme está dentro de uma Watchlist.
 *
 * Por que um enum e não uma String solta?
 * - O compilador só deixa usar os 3 valores abaixo: não existe "Visto", "visto " ou "VISTOO" por engano.
 * - O Swagger lista automaticamente os valores aceitos.
 *
 * Quem usa: WatchlistItem.status (mapeado com @Enumerated(EnumType.STRING)).
 */
@Schema(
        description = "Situação do filme na lista: QUERO_VER, VENDO, VISTO ou ABANDONADO",
        example = "QUERO_VER",
        requiredMode = Schema.RequiredMode.REQUIRED
)
public enum WatchStatus {

    /** Filme adicionado à lista, mas a pessoa ainda não começou a assistir. */
    QUERO_VER,

    /** A pessoa está assistindo no momento. */
    VENDO,

    /** A pessoa terminou o filme. Só neste estado faz sentido ter uma nota (regra a implementar no Service). */
    VISTO,

    ABANDONADO,
}