package com.cinelog.api.Service;

import com.cinelog.api.DTO.MovieDetailsPatchRequest;
import com.cinelog.api.DTO.MovieDetailsRequest;
import com.cinelog.api.DTO.MovieDetailsResponse;
import com.cinelog.api.Entity.Movie;
import com.cinelog.api.Entity.MovieDetails;
import com.cinelog.api.Exception.BusinessException;
import com.cinelog.api.Exception.ConflictException;
import com.cinelog.api.Exception.ResourceNotFoundException;
import com.cinelog.api.Repository.MovieDetailsRepository;
import com.cinelog.api.Repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * MovieDetailsService: regras de negócio dos detalhes de um filme (relacionamento 1:1 com Movie).
 */
@Service
public class MovieDetailsService {

    private final MovieDetailsRepository movieDetailsRepository;

    // Precisamos do MovieRepository para achar o filme dono dos detalhes
    private final MovieRepository movieRepository;

    public MovieDetailsService(MovieDetailsRepository movieDetailsRepository, MovieRepository movieRepository) {
        this.movieDetailsRepository = movieDetailsRepository;
        this.movieRepository = movieRepository;
    }

    // CREATE: Cadastra os detalhes de um filme
    public MovieDetailsResponse createMovieDetails(MovieDetailsRequest request) {

        // O filme informado precisa existir (erro 404)
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado com o ID: " + request.movieId()));

        // Regra do 1:1: cada filme só pode ter UM registro de detalhes (erro 409)
        if (movieDetailsRepository.existsByMovieId(request.movieId())) {
            throw new ConflictException("O filme com ID " + request.movieId() + " já possui detalhes cadastrados");
        }

        MovieDetails details = new MovieDetails(
                movie,
                request.sinopseLonga(),
                request.orcamento(),
                request.paisOrigem(),
                request.idiomaOriginal(),
                request.notasProducao());

        MovieDetails savedDetails = movieDetailsRepository.save(details);

        return toResponse(savedDetails);
    }

    // READ: Busca os detalhes pelo ID dos detalhes
    public MovieDetailsResponse findById(long id) {
        MovieDetails details = movieDetailsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Detalhes não encontrados com o ID: " + id));

        return toResponse(details);
    }

    // READ: Lista todos os detalhes de forma paginada
    public Page<MovieDetailsResponse> findAll(Pageable pageable) {
        Page<MovieDetails> detailsPage = movieDetailsRepository.findAll(pageable);

        return detailsPage.map(details -> toResponse(details));
    }

    // READ (consulta personalizada): detalhes de filmes de um país de origem
    public Page<MovieDetailsResponse> findByCountry(String paisOrigem, Pageable pageable) {
        Page<MovieDetails> detailsPage = movieDetailsRepository.findByPaisOrigemContainingIgnoreCase(paisOrigem, pageable);

        return detailsPage.map(details -> toResponse(details));
    }

    // READ (consulta personalizada): detalhes de um filme específico
    public MovieDetailsResponse findByMovieId(long movieId) {
        MovieDetails details = movieDetailsRepository.findByMovieId(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Detalhes não encontrados para o filme com ID: " + movieId));

        return toResponse(details);
    }

    // UPDATE: Atualiza os detalhes existentes
    public MovieDetailsResponse updateMovieDetails(long id, MovieDetailsRequest request) {
        MovieDetails details = movieDetailsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Detalhes não encontrados com o ID: " + id));

        // Regra: os detalhes não "mudam de filme". O movieId enviado precisa ser o mesmo do registro (erro 400)
        if (!details.getMovie().getId().equals(request.movieId())) {
            throw new BusinessException("Não é permitido trocar o filme dos detalhes. Envie o mesmo movieId do registro");
        }

        details.setSinopseLonga(request.sinopseLonga());
        details.setOrcamento(request.orcamento());
        details.setPaisOrigem(request.paisOrigem());
        details.setIdiomaOriginal(request.idiomaOriginal());
        details.setNotasProducao(request.notasProducao());

        MovieDetails savedDetails = movieDetailsRepository.save(details);

        return toResponse(savedDetails);
    }

    // UPDATE PARCIAL: atualiza só os campos que vierem preenchidos no JSON
    public MovieDetailsResponse patchMovieDetails(long id, MovieDetailsPatchRequest request) {
        MovieDetails details = movieDetailsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Detalhes não encontrados com o ID: " + id));

        if (request.sinopseLonga() != null) {
            details.setSinopseLonga(request.sinopseLonga());
        }
        if (request.orcamento() != null) {
            details.setOrcamento(request.orcamento());
        }
        if (request.paisOrigem() != null) {
            details.setPaisOrigem(request.paisOrigem());
        }
        if (request.idiomaOriginal() != null) {
            details.setIdiomaOriginal(request.idiomaOriginal());
        }
        if (request.notasProducao() != null) {
            details.setNotasProducao(request.notasProducao());
        }

        MovieDetails savedDetails = movieDetailsRepository.save(details);

        return toResponse(savedDetails);
    }

    // DELETE: Apaga os detalhes (o filme continua existindo)
    public void deleteMovieDetails(long id) {
        if (!movieDetailsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Detalhes não encontrados com o ID: " + id);
        }

        movieDetailsRepository.deleteById(id);
    }

    // Converte a entidade MovieDetails no DTO de saída
    private MovieDetailsResponse toResponse(MovieDetails details) {
        return new MovieDetailsResponse(
                details.getId(),
                details.getMovie().getId(),
                details.getSinopseLonga(),
                details.getOrcamento(),
                details.getPaisOrigem(),
                details.getIdiomaOriginal(),
                details.getNotasProducao());
    }
}
