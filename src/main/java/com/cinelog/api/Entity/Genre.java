package com.cinelog.api.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Genre: um gênero de filme (Ação, Drama, Comédia...).
 * Cada linha da tabela "genres" é um gênero. Um filme pode ter vários gêneros e um gênero
 * pode estar em vários filmes (muitos-para-muitos). Esse relacionamento é configurado
 * na classe Movie, então aqui não precisa de nada além dos campos do próprio gênero.
 */
@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // o banco gera o id sozinho (1, 2, 3...)
    private Long id;

    // @NotBlank e @Size: validam o dado no Java, antes de salvar.
    // nullable = false: a coluna no banco não aceita vazio (NULL).
    // unique = true: o banco não deixa cadastrar dois gêneros com o mesmo nome.
    @NotBlank(message = "O nome do gênero não pode estar em branco")
    @Size(min = 4 ,max = 50, message = "O nome do gênero deve ter 4 a 50 caracteres")
    @Column(nullable = false, unique = true)
    private String nome;

    // O JPA exige um construtor vazio: ele cria o objeto vazio e depois preenche com os dados do banco
    public Genre() {
    }

    public Genre(String nome) {
        this.nome = nome;
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

    @Override
    public String toString() {
        return "Genre{id=" + id + ", nome='" + nome + "'}";
    }
}