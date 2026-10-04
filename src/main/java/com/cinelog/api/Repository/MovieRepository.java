package com.cinelog.api.Repository;

import com.cinelog.api.Entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * MovieRepository: acesso ao banco para a entidade Movie.
 * Os métodos básicos e o findAll(Pageable) já vêm do JpaRepository.
 */
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Busca personalizada: filmes cujo título contém o texto informado (ignora maiúsculas/minúsculas)
    Page<Movie> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    // Busca personalizada: filmes de um gênero. "Genres" é o campo da Movie e "Id" é o id do gênero,
    // então o Spring entende como "movie.genres.id = genreId" e usa a tabela movie_genres sozinho.
    Page<Movie> findByGenresId(Long genreId, Pageable pageable);

    boolean existsByGenresId(long id);
}
