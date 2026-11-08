package com.cinelog.api.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * MovieResponseV2: DTO de SAÍDA do filme na VERSÃO 2 da API (cabeçalho X-API-Version: 2).
 *
 * Diferenças em relação à versão 1 (MovieResponse):
 * - o campo "duracao" passou a se chamar "duracaoMinutos" (deixa a unidade explícita);
 * - "generos" agora é uma lista só com os NOMES (texto), em vez de objetos com id e nome.
 * Como são mudanças que quebrariam quem já usa a v1, elas só valem para quem pede a versão 2.
 */
@Schema(description = "Representação do filme na versão 2 da API (X-API-Version: 2)")
public record MovieResponseV2(

        @Schema(description = "Identificador único gerado pelo banco de dados", example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        long id,

        @Schema(description = "Título do filme", example = "Interestelar")
        String titulo,

        @Schema(description = "Ano de lançamento", example = "2014")
        int anoLancamento,

        @Schema(description = "Duração do filme em minutos (na versão 1 este campo se chamava 'duracao')", example = "169")
        int duracaoMinutos,

        @Schema(description = "Classificação indicativa", example = "10")
        String classificacaoIndicativa,

        @Schema(description = "Nomes dos gêneros do filme (na versão 1 eram objetos com id e nome)",
                example = "[\"Ficção Científica\", \"Drama\"]")
        List<String> generos
) {

    // Converte a resposta da v1 (que o Service já devolve) para o formato da v2.
    public static MovieResponseV2 from(MovieResponse movie) {
        List<String> nomes = movie.generos() == null
                ? List.of()
                : movie.generos().stream().map(GenreResponse::nome).toList();

        return new MovieResponseV2(
                movie.id(),
                movie.titulo(),
                movie.anoLancamento(),
                movie.duracao(),
                movie.classificacaoIndicativa(),
                nomes
        );
    }
}
