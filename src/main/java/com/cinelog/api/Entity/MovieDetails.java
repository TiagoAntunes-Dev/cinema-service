package com.cinelog.api.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * MovieDetails: informações detalhadas de um filme, guardadas em uma tabela separada da Movie.
 * Assim, as listagens de filmes ficam leves e só buscamos os detalhes quando alguém pede.
 *
 * Relacionamento um-para-um: cada filme tem no máximo UM registro de detalhes,
 * e cada registro de detalhes pertence a UM filme.
 */
@Entity
@Table(name = "movie_details")
public class MovieDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UM-PARA-UM com Movie.
    // @JoinColumn cria na tabela movie_details a coluna "movie_id" (chave estrangeira que aponta para movies).
    // unique = true é o que garante o "um-para-um": o banco não deixa dois detalhes apontarem para o mesmo filme.
    // nullable = false: detalhes sem filme não fazem sentido.
    // Só esta classe conhece a Movie; a Movie não tem campo para os detalhes (não precisa).
    @OneToOne
    @JoinColumn(name = "movie_id", nullable = false, unique = true)
    private Movie movie;

    // O tamanho padrão de uma coluna de texto é 255 caracteres. Como a sinopse é longa,
    // aumentamos com length. O @Size valida o mesmo limite no Java.
    @Size(max = 4000, message = "A sinopse deve ter no máximo 4000 caracteres")
    @Column(length = 4000)
    private String sinopseLonga;

    // BigDecimal é o tipo recomendado para dinheiro (double tem erros de arredondamento)
    @PositiveOrZero(message = "O orçamento não pode ser negativo")
    private BigDecimal orcamento;

    @Size(max = 60, message = "O país de origem deve ter no máximo 60 caracteres")
    private String paisOrigem;

    @Size(max = 40, message = "O idioma original deve ter no máximo 40 caracteres")
    private String idiomaOriginal;

    @Size(max = 2000, message = "As notas de produção devem ter no máximo 2000 caracteres")
    @Column(length = 2000)
    private String notasProducao;

    public MovieDetails() {
    }

    public MovieDetails(Movie movie, String sinopseLonga, BigDecimal orcamento,
                        String paisOrigem, String idiomaOriginal, String notasProducao) {
        this.movie = movie;
        this.sinopseLonga = sinopseLonga;
        this.orcamento = orcamento;
        this.paisOrigem = paisOrigem;
        this.idiomaOriginal = idiomaOriginal;
        this.notasProducao = notasProducao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public String getSinopseLonga() {
        return sinopseLonga;
    }

    public void setSinopseLonga(String sinopseLonga) {
        this.sinopseLonga = sinopseLonga;
    }

    public BigDecimal getOrcamento() {
        return orcamento;
    }

    public void setOrcamento(BigDecimal orcamento) {
        this.orcamento = orcamento;
    }

    public String getPaisOrigem() {
        return paisOrigem;
    }

    public void setPaisOrigem(String paisOrigem) {
        this.paisOrigem = paisOrigem;
    }

    public String getIdiomaOriginal() {
        return idiomaOriginal;
    }

    public void setIdiomaOriginal(String idiomaOriginal) {
        this.idiomaOriginal = idiomaOriginal;
    }

    public String getNotasProducao() {
        return notasProducao;
    }

    public void setNotasProducao(String notasProducao) {
        this.notasProducao = notasProducao;
    }

    @Override
    public String toString() {
        return "MovieDetails{id=" + id + ", paisOrigem='" + paisOrigem + "', idiomaOriginal='" + idiomaOriginal + "'}";
    }
}
