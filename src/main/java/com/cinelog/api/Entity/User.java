package com.cinelog.api.Entity; // Define o pacote onde ficam as classes que representam as tabelas do banco de dados

import jakarta.persistence.*;

/**
 * @Entity: conecta a Programação Orientada a Objetos (classes, atributos e tipos Java)
 * à estrutura relacional usado para mapear dados na database (tabelas, colunas)
 *
/**
 * @Table(name = "users"): É uma boa prática forçar o nome da tabela no plural ("users").
 * Se não usarmos isso, o banco tentaria criar uma tabela chamada "user", o que
 * muitas vezes dá erro de sintaxe, pois "USER" é uma palavra reservada em muitos bancos SQL.
 */
@Table(name = "users")
public class User {

    /**
     * @Id: Define que este atributo é a Chave Primária (Primary Key) da tabela.
     * @GeneratedValue(strategy = GenerationType.IDENTITY): Diz ao banco de dados (H2, MySQL)
     * para usar o recurso de "Auto-Incremento". Ou seja, o banco gera o ID sozinho (1, 2, 3...).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @Column: Opcional, mas explícito. Garante que este atributo seja uma coluna na tabela.
     * Como não passamos parâmetros (ex: name="nome_usuario"), a coluna terá o mesmo nome do atributo ("name").
     */
    @Column
    private String name;

    @Column
    private String email;

    /**
     * Construtor Vazio (No-args constructor):
     * EXTREMAMENTE IMPORTANTE! O JPA/Hibernate exige um construtor vazio para conseguir
     * criar o objeto a partir dos dados do banco usando um recurso do Java chamado "Reflection".
     */
    public User() {
    }

    /**
     * Construtor com parâmetros:
     * Criado para facilitar a nossa vida quando precisarmos transformar um DTO em Entidade
     * lá no UserService (não passamos o ID porque ele é gerado pelo banco).
     */
    public User(String email, String name) {
        this.email = email;
        this.name = name;
    }

    // Getters e Setters: Necessários para o Spring, Hibernate e Jackson acessarem e modificarem os atributos privados.
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /**
     * toString(): Útil para depuração (debug). Se você imprimir o objeto no console (System.out.println),
     * ele mostrará os dados formatados em vez de mostrar o endereço de memória do objeto.
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}