package com.cinelog.api.Service;

import com.cinelog.api.DTO.GenreResponse;
import com.cinelog.api.DTO.MovieRequest;
import com.cinelog.api.DTO.MovieResponse;
import com.cinelog.api.Entity.Genre;
import com.cinelog.api.Entity.Movie;
import com.cinelog.api.Exception.ConflictException;
import com.cinelog.api.Exception.ResourceNotFoundException;
import com.cinelog.api.Repository.GenreRepository;
import com.cinelog.api.Repository.MovieDetailsRepository;
import com.cinelog.api.Repository.MovieRepository;
import com.cinelog.api.Repository.WatchListItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MovieService: regras de negócio dos filmes.
 */
@Service
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieDetailsRepository movieDetailsRepository;
    private final WatchListItemRepository watchlistItemRepository;

    public MovieService(MovieRepository movieRepository,
                        GenreRepository genreRepository,
                        MovieDetailsRepository movieDetailsRepository,
                        WatchListItemRepository watchlistItemRepository) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieDetailsRepository = movieDetailsRepository;
        this.watchlistItemRepository = watchlistItemRepository;
    }

    // CREATE: Cadastra um novo filme
    public MovieResponse createMovie(MovieRequest movieRequest) {
        Movie movie = new Movie(
                movieRequest.titulo(),
                movieRequest.anoLancamento(),
                movieRequest.duracao(),
                movieRequest.classificacaoIndicativa());

        movie.setGenres(findGenres(movieRequest.genreIds()));

        Movie savedMovie = movieRepository.save(movie);

        return toResponse(savedMovie);
    }

    // READ: Busca um filme pelo ID
    @Transactional(readOnly = true)
    public MovieResponse findById(long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado com o ID: " + id));

        return toResponse(movie);
    }

    // READ: Lista os filmes de forma paginada
    @Transactional(readOnly = true)
    public Page<MovieResponse> findAll(Pageable pageable) {
        Page<Movie> movies = movieRepository.findAll(pageable);

        return movies.map(this::toResponse);
    }

    // READ: Filmes cujo título contém o texto informado
    @Transactional(readOnly = true)
    public Page<MovieResponse> findByTitle(String titulo, Pageable pageable) {
        Page<Movie> movies = movieRepository.findByTituloContainingIgnoreCase(titulo, pageable);

        return movies.map(this::toResponse);
    }

    // READ: Filmes de um gênero
    @Transactional(readOnly = true)
    public Page<MovieResponse> findByGenre(long genreId, Pageable pageable) {
        if (!genreRepository.existsById(genreId)) {
            throw new ResourceNotFoundException("Gênero não encontrado com o ID: " + genreId);
        }

        Page<Movie> movies = movieRepository.findByGenresId(genreId, pageable);

        return movies.map(this::toResponse);
    }

    // UPDATE: Atualiza um filme existente
    public MovieResponse updateMovie(long id, MovieRequest movieRequest) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado com o ID: " + id));

        movie.setTitulo(movieRequest.titulo());
        movie.setAnoLancamento(movieRequest.anoLancamento());
        movie.setDuracao(movieRequest.duracao());
        movie.setClassificacaoIndicativa(movieRequest.classificacaoIndicativa());

        movie.setGenres(findGenres(movieRequest.genreIds()));

        Movie savedMovie = movieRepository.save(movie);

        return toResponse(savedMovie);
    }

    // DELETE: Apaga um filme
    public void deleteMovie(long id) {

        // Verifica se existe → 404 se não.
        if (!movieRepository.existsById(id)) {
            throw new ResourceNotFoundException("Filme não encontrado com o ID: " + id);
        }

        // Verifica watchlist → 409 se em uso.
        if (watchlistItemRepository.existsByMovieId(id)) {
            throw new ConflictException("Não é possível excluir o filme com ID " + id + ", pois ele está em uma ou mais watchlists");
        }
        // ⭐ Deleta os detalhes ANTES de deletar o filme
        movieDetailsRepository.findByMovieId(id).ifPresent(movieDetailsRepository::delete);

        // Deleta o filme
        movieRepository.deleteById(id);
    }

    private Set<Genre> findGenres(Set<Long> genreIds) {
        Set<Genre> genres = new HashSet<>();

        if (genreIds != null) {
            for (Long genreId : genreIds) {
                Genre genre = genreRepository.findById(genreId)
                        .orElseThrow(() -> new ResourceNotFoundException("Gênero não encontrado com o ID: " + genreId));
                genres.add(genre);
            }
        }

        return genres;
    }

    // Converte a entidade Movie no DTO de saída MovieResponse
    private MovieResponse toResponse(Movie movie) {
        List<GenreResponse> generos = new ArrayList<>();

        for (Genre genre : movie.getGenres()) {
            generos.add(new GenreResponse(genre.getId(), genre.getNome()));
        }

        return new MovieResponse(
                movie.getId(),
                movie.getTitulo(),
                movie.getAnoLancamento(),
                movie.getDuracao(),
                movie.getClassificacaoIndicativa(),
                generos);
    }
}
