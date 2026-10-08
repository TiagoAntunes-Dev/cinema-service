package com.cinelog.api.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Movie: os dados básicos de um filme (os que aparecem nas listagens).
 * Os dados mais longos (sinopse, orçamento, etc.) ficam em outra tabela, a MovieDetails.
 *
 * Relacionamentos:
 * - Muitos-para-muitos com Genre (campo "genres", configurado aqui).
 * - Um-para-um com MovieDetails (configurado na classe MovieDetails).
 */
@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título não pode estar em branco")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
    @Column(nullable = false)
    private String titulo;

    // Integer (e não int) porque o int nunca é nulo: o @NotNull só consegue detectar "campo não enviado" em Integer.
    // 1888 é o ano do primeiro filme já registrado.
    @NotNull(message = "O ano de lançamento é obrigatório")
    @Min(value = 1888, message = "O ano de lançamento deve ser a partir de 1888")
    @Max(value = 2050, message = "O ano de lançamento deve ser no máximo 2050")
    @Column(nullable = false)
    private Integer anoLancamento;

    // Duração em minutos
    @NotNull(message = "A duração é obrigatória")
    @Positive(message = "A duração deve ser maior que zero")
    @Column(nullable = false)
    private Integer duracao;

    // Classificação indicativa em texto, porque inclui "L" (livre). Exemplos: L, 10, 12, 14, 16, 18
    @NotBlank(message = "A classificação indicativa é obrigatória")
    @Size(max = 3, message = "A classificação indicativa deve ter no máximo 3 caracteres")
    @Column(nullable = false)
    private String classificacaoIndicativa;

    // MUITOS-PARA-MUITOS: um filme tem vários gêneros e um gênero está em vários filmes.
    // Um banco relacional não guarda isso em uma coluna só, então o JPA cria uma tabela
    // intermediária. O @JoinTable define como ela é:
    //   name = nome da tabela intermediária (movie_genres)
    //   joinColumns = coluna que aponta para ESTA classe (movie_id -> movies)
    //   inverseJoinColumns = coluna que aponta para a OUTRA classe (genre_id -> genres)
    // Usamos Set porque ele não aceita itens repetidos (o mesmo gênero não entra duas vezes).
    // O "= new HashSet<>()" começa a coleção vazia, para ela nunca ser null.
    @ManyToMany
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )

    // Sem Duplicata
    private Set<Genre> genres = new HashSet<>();

    public Movie() {
    }

    public Movie(String titulo, Integer anoLancamento, Integer duracao, String classificacaoIndicativa) {
        this.titulo = titulo;
        this.anoLancamento = anoLancamento;
        this.duracao = duracao;
        this.classificacaoIndicativa = classificacaoIndicativa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getAnoLancamento() {
        return anoLancamento;
    }

    public void setAnoLancamento(Integer anoLancamento) {
        this.anoLancamento = anoLancamento;
    }

    public Integer getDuracao() {
        return duracao;
    }

    public void setDuracao(Integer duracao) {
        this.duracao = duracao;
    }

    public String getClassificacaoIndicativa() {
        return classificacaoIndicativa;
    }

    public void setClassificacaoIndicativa(String classificacaoIndicativa) {
        this.classificacaoIndicativa = classificacaoIndicativa;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    @Override
    public String toString() {
        return "Movie{id=" + id + ", titulo='" + titulo + "', anoLancamento=" + anoLancamento + "}";
    }
}
