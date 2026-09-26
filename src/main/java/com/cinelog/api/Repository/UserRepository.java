package com.cinelog.api.Repository; // Define o pacote responsável pela comunicação direta com o banco de dados

import com.cinelog.api.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * UserRepository: Interface que gerencia o acesso aos dados da entidade User.
 *
 * extends JpaRepository<User, Long>:
 * - 'User' é a entidade que esta interface vai gerenciar.
 * - 'Long' é o tipo de dado da Chave Primária (@Id) da entidade User.
 *
 * O simples fato de estender essa interface já entrega de bandeja todos os métodos
 * de banco de dados (save, findById, findAll, deleteById) gerados em tempo de execução pelo Spring Data JPA.
 */
public interface UserRepository extends JpaRepository<User, Long> {
}