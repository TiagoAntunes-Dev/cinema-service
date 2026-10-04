package com.cinelog.api.Repository;

import com.cinelog.api.Entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * GenreRepository: acesso ao banco para a entidade Genre.
 *
 * Os métodos básicos (save, findById, findAll, deleteById...) já vêm do JpaRepository.
 * O findAll(Pageable) também vem pronto e é ele que faz as listagens paginadas.
 *
 * Os métodos abaixo são "derivados": o Spring lê o NOME do método e monta o SQL sozinho.
 */
public interface GenreRepository extends JpaRepository<Genre, Long> {

    // Busca personalizada: gêneros cujo nome contém o texto informado, sem diferenciar maiúsculas de minúsculas.
    // Containing = LIKE %texto% | IgnoreCase = ignora maiúsculas/minúsculas | Pageable = resultado paginado
    Page<Genre> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // Verifica se já existe um gênero com esse nome (para devolver erro 409 em vez de erro do banco)
    boolean existsByNomeIgnoreCase(String nome);
}