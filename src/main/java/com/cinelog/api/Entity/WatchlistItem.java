package com.cinelog.api.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * WatchlistItem: um filme dentro de uma watchlist, junto com as informações dessa ligação
 * (o status e a nota que o usuário deu).
 *
 * Por que uma entidade própria, e não um @ManyToMany direto entre Watchlist e Movie?
 * Porque um @ManyToMany só cria a tabela do meio com as duas chaves estrangeiras, sem espaço
 * para colunas extras. Como precisamos guardar "status" e "nota", essa tabela do meio vira
 * uma entidade com dois @ManyToOne: um para a Watchlist e outro para o Movie.
 */
@Entity
@Table(name = "watchlist_items")
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MUITOS-PARA-UM: vários itens pertencem a uma watchlist.
    // Esta classe guarda a chave estrangeira "watchlist_id" (é o campo citado no mappedBy da Watchlist).
    @ManyToOne
    @JoinColumn(name = "watchlist_id", nullable = false)
    private WatchList watchlist;

    // MUITOS-PARA-UM: o mesmo filme pode aparecer em itens de várias watchlists.
    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    // @Enumerated(EnumType.STRING) grava o NOME do enum no banco (por exemplo, "VISTO").
    // Se usássemos o padrão (ORDINAL), o banco guardaria só o número (0, 1, 2), e se alguém mudasse
    // a ordem dos valores do enum, os dados antigos passariam a significar outra coisa.
    // O valor inicial QUERO_VER é usado quando o status não é informado.
    @NotNull(message = "O status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchStatus status = WatchStatus.QUERO_VER;

    // Nota de 1 a 10. É opcional: sem nota, o valor fica null (os validadores @Min e @Max ignoram null).
    @Min(value = 1, message = "A nota deve ser no mínimo 1")
    @Max(value = 10, message = "A nota deve ser no máximo 10")
    private Integer nota;

    public WatchlistItem() {
    }

    public WatchlistItem(WatchList watchlist, Movie movie) {
        this.watchlist = watchlist;
        this.movie = movie;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WatchList getWatchlist() {
        return watchlist;
    }

    public void setWatchlist(WatchList watchlist) {
        this.watchlist = watchlist;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public WatchStatus getStatus() {
        return status;
    }

    public void setStatus(WatchStatus status) {
        this.status = status;
    }

    public Integer getNota() {
        return nota;
    }

    public void setNota(Integer nota) {
        this.nota = nota;
    }

    @Override
    public String toString() {
        return "WatchlistItem{id=" + id + ", status=" + status + ", nota=" + nota + "}";
    }
}
