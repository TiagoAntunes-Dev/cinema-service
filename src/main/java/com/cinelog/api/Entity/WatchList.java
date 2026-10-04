package com.cinelog.api.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Watchlist: uma lista de filmes que um usuário monta (ex.: "Para ver no fim de semana").
 *
 * Relacionamentos:
 * - Um usuário tem VÁRIAS watchlists. No banco, a chave estrangeira (user_id) fica nesta tabela,
 *   por isso aqui usamos @ManyToOne (muitas watchlists para um usuário).
 * - Uma watchlist tem VÁRIOS itens (WatchlistItem): é o um-para-muitos do projeto.
 */
@Entity
@Table(name = "watchlists")
public class WatchList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da watchlist não pode estar em branco")
    @Size(min = 3, max = 100, message = "O nome da watchlist deve ter entre 3 e 100 caracteres")
    @Column(nullable = false)
    private String nome;

    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    private String descricao;

    // MUITOS-PARA-UM: várias watchlists pertencem a um mesmo usuário.
    // @JoinColumn cria a coluna "user_id" (chave estrangeira para a tabela users).
    // nullable = false: toda watchlist precisa ter um dono.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // UM-PARA-MUITOS: uma watchlist tem vários itens.
    // mappedBy = "watchlist" significa: "quem guarda a chave estrangeira é o campo 'watchlist'
    // da classe WatchlistItem". Por isso esta tabela não ganha coluna nenhuma por causa deste campo;
    // ele só serve para enxergar os itens a partir da watchlist.
    // Sem cascade: salvar a watchlist NÃO salva os itens. Cada item é salvo por conta própria
    // (item.setWatchlist(watchlist) e depois o repository do item).
    @OneToMany(mappedBy = "watchlist")
    private List<WatchlistItem> items = new ArrayList<>();

    public WatchList() {
    }

    public WatchList(String nome, String descricao, User user) {
        this.nome = nome;
        this.descricao = descricao;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<WatchlistItem> getItems() {
        return items;
    }

    public void setItems(List<WatchlistItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "Watchlist{id=" + id + ", nome='" + nome + "'}";
    }
}
