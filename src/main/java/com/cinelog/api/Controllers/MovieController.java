package com.cinelog.api.Controllers; // Define o pacote onde o Controller está localizado

import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.MovieRequest;
import com.cinelog.api.DTO.MovieResponse;
import com.cinelog.api.Service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

// Importa todos os métodos estáticos do WebMvcLinkBuilder:
// linkTo(...), methodOn(...) — usados para gerar os links HATEOAS.
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * MovieController: recebe as requisições HTTP sobre filmes e delega o trabalho ao MovieService.
 * Rotas no plural: "/movies".
 *
 * HATEOAS: as respostas agora vêm envoltas em EntityModel (recurso único) ou PagedModel (listas paginadas).
 * Cada resposta inclui um bloco "_links" com self, update, delete, all e navegabilidade para recursos relacionados.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Filmes", description = "Endpoints responsáveis pela gestão do catálogo de filmes")
public class MovieController {

    private final MovieService movieService;

    // PagedResourcesAssembler: bean do Spring HATEOAS que transforma um Page<T> em PagedModel<T>,
    // já adicionando automaticamente os links de paginação (self, first, last, next, prev).
    private final PagedResourcesAssembler<MovieResponse> pagedAssembler;

    public MovieController(MovieService movieService,
                           PagedResourcesAssembler<MovieResponse> pagedAssembler) {
        this.movieService = movieService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping("/movies")
    @Operation(summary = "Criar um novo filme", description = "Cadastra um filme. Os gêneros são opcionais e enviados pelos IDs (genreIds); cada gênero precisa já estar cadastrado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Filme criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (título vazio, ano anterior a 1888, duração não positiva...)",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Algum dos gêneros informados não existe",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieResponse>> createMovie(@Valid @RequestBody MovieRequest movieRequest) {
        MovieResponse movieResponse = movieService.createMovie(movieRequest);

        // Envolve o DTO em um EntityModel e adiciona os links.
        // "details" só é incluído se o filme tiver detalhes — senão o link daria 404.
        // Como o DTO MovieResponse não traz essa informação, deixamos o link sempre presente
        // (o cliente descobre que não há detalhes ao seguir o link).
        EntityModel<MovieResponse> model = EntityModel.of(movieResponse,
                linkTo(methodOn(MovieController.class).getMovie(movieResponse.id())).withSelfRel(),
                linkTo(methodOn(MovieController.class).getAllMovies(null)).withRel("all-movies"),
                linkTo(methodOn(MovieDetailsController.class).getMovieDetailsByMovie(movieResponse.id())).withRel("details")
        );

        // Status 201 (Created) + cabeçalho "Location" com a URI do novo recurso + DTO com links no corpo
        return ResponseEntity
                .created(URI.create("/api/v1/movies/" + movieResponse.id()))
                .body(model);
    }

    @GetMapping("/movies/{id}")
    @Operation(summary = "Buscar filme por ID", description = "Retorna os dados de um filme, incluindo os gêneros.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Filme não encontrado para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieResponse>> getMovie(
            @Parameter(description = "ID numérico do filme a ser buscado", example = "1")
            @PathVariable long id) {
        MovieResponse movieResponse = movieService.findById(id);

        // Navegabilidade: cada filme aponta para seus detalhes, seus gêneros e pode ser atualizado/deletado.
        EntityModel<MovieResponse> model = EntityModel.of(movieResponse,
                linkTo(methodOn(MovieController.class).getMovie(id)).withSelfRel(),
                linkTo(methodOn(MovieController.class).getAllMovies(null)).withRel("all-movies"),
                linkTo(methodOn(MovieController.class).updateMovie(id, null)).withRel("update"),
                linkTo(methodOn(MovieController.class).deleteMovie(id)).withRel("delete"),
                linkTo(methodOn(MovieDetailsController.class).getMovieDetailsByMovie(id)).withRel("details")
        );

        return ResponseEntity.ok(model);
    }

    @GetMapping("/movies")
    @Operation(summary = "Listar filmes", description = "Retorna os filmes cadastrados de forma paginada. Use os parâmetros page, size e sort (ex.: sort=titulo,asc).")
    @ApiResponse(responseCode = "200", description = "Página de filmes retornada com sucesso")
    public ResponseEntity<PagedModel<EntityModel<MovieResponse>>> getAllMovies(@ParameterObject Pageable pageable) {
        Page<MovieResponse> page = movieService.findAll(pageable);

        // PagedModel inclui automaticamente os links de paginação (self, first, last, next, prev).
        PagedModel<EntityModel<MovieResponse>> model = pagedAssembler.toModel(
                page,
                movieResponse -> EntityModel.of(movieResponse,
                        linkTo(methodOn(MovieController.class).getMovie(movieResponse.id())).withSelfRel()
                )
        );

        return ResponseEntity.ok(model);
    }

    // Consulta personalizada 1: busca por título (parte do título, sem diferenciar maiúsculas de minúsculas)
    @GetMapping("/movies/search")
    @Operation(summary = "Buscar filmes por título", description = "Retorna, de forma paginada, os filmes cujo título contém o texto informado.")
    @ApiResponse(responseCode = "200", description = "Página de filmes retornada com sucesso (pode vir vazia)")
    public ResponseEntity<PagedModel<EntityModel<MovieResponse>>> searchMovies(
            @Parameter(description = "Parte do título do filme", example = "inter")
            @RequestParam String titulo,
            @ParameterObject Pageable pageable) {
        Page<MovieResponse> page = movieService.findByTitle(titulo, pageable);

        PagedModel<EntityModel<MovieResponse>> model = pagedAssembler.toModel(
                page,
                movieResponse -> EntityModel.of(movieResponse,
                        linkTo(methodOn(MovieController.class).getMovie(movieResponse.id())).withSelfRel()
                )
        );

        return ResponseEntity.ok(model);
    }

    // Consulta personalizada 2: rota aninhada. Lê-se "os filmes do gênero X": /genres/1/movies
    @GetMapping("/genres/{genreId}/movies")
    @Operation(summary = "Listar filmes de um gênero", description = "Retorna, de forma paginada, os filmes que pertencem ao gênero informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de filmes retornada com sucesso (pode vir vazia)"),
            @ApiResponse(responseCode = "404", description = "Gênero não encontrado para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<PagedModel<EntityModel<MovieResponse>>> getMoviesByGenre(
            @Parameter(description = "ID numérico do gênero", example = "1")
            @PathVariable long genreId,
            @ParameterObject Pageable pageable) {
        Page<MovieResponse> page = movieService.findByGenre(genreId, pageable);

        PagedModel<EntityModel<MovieResponse>> model = pagedAssembler.toModel(
                page,
                movieResponse -> EntityModel.of(movieResponse,
                        linkTo(methodOn(MovieController.class).getMovie(movieResponse.id())).withSelfRel(),
                        // Navegabilidade reversa: do filme, volta para o gênero dele
                        linkTo(methodOn(GenreController.class).getGenre(genreId)).withRel("genre")
                )
        );

        return ResponseEntity.ok(model);
    }

    @PutMapping("/movies/{id}")
    @Operation(summary = "Atualizar filme", description = "Atualiza todos os dados de um filme existente. Os gêneros enviados SUBSTITUEM os gêneros atuais.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erros de validação nos campos informados",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Filme ou gênero não encontrado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieResponse>> updateMovie(
            @Parameter(description = "ID numérico do filme a ser atualizado", example = "1")
            @PathVariable long id,
            @Valid @RequestBody MovieRequest movieRequest) {
        MovieResponse movieResponse = movieService.updateMovie(id, movieRequest);

        EntityModel<MovieResponse> model = EntityModel.of(movieResponse,
                linkTo(methodOn(MovieController.class).getMovie(id)).withSelfRel(),
                linkTo(methodOn(MovieController.class).getAllMovies(null)).withRel("all-movies"),
                linkTo(methodOn(MovieController.class).deleteMovie(id)).withRel("delete"),
                linkTo(methodOn(MovieDetailsController.class).getMovieDetailsByMovie(id)).withRel("details")
        );

        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/movies/{id}")
    @Operation(summary = "Deletar filme", description = "Remove um filme (e os detalhes dele). Não é possível remover um filme que está em alguma watchlist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Filme deletado com sucesso (sem corpo de resposta)"),
            @ApiResponse(responseCode = "404", description = "Filme não encontrado para deleção",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "O filme está em uma ou mais watchlists e não pode ser removido",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<Void> deleteMovie(
            @Parameter(description = "ID numérico do filme a ser excluído", example = "2")
            @PathVariable long id) {
        movieService.deleteMovie(id);

        // Status 204 (No Content): deu certo, sem corpo na resposta (portanto, sem links)
        return ResponseEntity.noContent().build();
    }
}