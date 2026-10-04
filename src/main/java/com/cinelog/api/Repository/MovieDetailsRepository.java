package com.cinelog.api.Repository;

import com.cinelog.api.Entity.MovieDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * MovieDetailsRepository: acesso ao banco para a entidade MovieDetails.
 * Os métodos básicos e o findAll(Pageable) já vêm do JpaRepository.
 */
public interface MovieDetailsRepository extends JpaRepository<MovieDetails, Long> {

    // Busca os detalhes de um filme. "Movie" é o campo da MovieDetails e "Id" é o id do filme
    // (movie.id). Optional porque um filme pode ainda não ter detalhes cadastrados.
    Optional<MovieDetails> findByMovieId(Long movieId);

    // Verifica se o filme já tem detalhes (o relacionamento é 1:1, então só pode existir um registro)
    boolean existsByMovieId(Long movieId);

    // Busca personalizada: detalhes de filmes de um país de origem (ignora maiúsculas/minúsculas)
    Page<MovieDetails> findByPaisOrigemContainingIgnoreCase(String paisOrigem, Pageable pageable);
}
