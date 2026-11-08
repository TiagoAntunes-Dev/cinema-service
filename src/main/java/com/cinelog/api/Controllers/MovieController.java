package com.cinelog.api.Controllers; // Define o pacote onde o Controller está localizado

import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.MovieRequest;
import com.cinelog.api.DTO.MovieResponse;
import com.cinelog.api.DTO.MovieResponseV2;
import com.cinelog.api.Service.MovieService;
import com.cinelog.api.Service.IdempotencyService;
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
@RequestMapping("/api")
@Tag(name = "Filmes", description = "Endpoints responsáveis pela gestão do catálogo de filmes")
public class MovieController {

    private final MovieService movieService;
    private final IdempotencyService idempotencyService;

    // PagedResourcesAssembler: bean do Spring HATEOAS que transforma um Page<T> em PagedModel<T>,
    // já adicionando automaticamente os links de paginação (self, first, last, next, prev).
    private final PagedResourcesAssembler<MovieResponse> pagedAssembler;

    public MovieController(MovieService movieService,
                           PagedResourcesAssembler<MovieResponse> pagedAssembler,
                           IdempotencyService idempotencyService) {
        this.movieService = movieService;
        this.pagedAssembler = pagedAssembler;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/movies")
    @Operation(summary = "Criar um novo filme", description = "Cadastra um filme. Os gêneros são opcionais e enviados pelos IDs (genreIds); cada gênero precisa já estar cadastrado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Filme criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (título vazio, ano anterior a 1888, duração não positiva...)",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Algum dos gêneros informados não existe",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "422", description = "A chave X-Idempotency-Key já foi usada com um corpo de requisição diferente",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieResponse>> createMovie(
            @Parameter(description = "Chave única (ex.: um UUID) que torna este POST idempotente. Repetir a mesma chave com o mesmo corpo devolve a resposta original, sem criar outro registro. Opcional.", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MovieRequest movieRequest) {

        // Idempotência: se a mesma chave X-Idempotency-Key chegar de novo com o mesmo corpo, o IdempotencyService
        // devolve a resposta guardada em vez de criar o registro outra vez. Sem o cabeçalho, cria normalmente.
        return idempotencyService.execute(idempotencyKey, movieRequest, () -> {
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
                    .created(URI.create("/api/movies/" + movieResponse.id()))
                    .body(model);
        });
    }

    // VERSIONAMENTO: esta é a versão 1 do endpoint (X-API-Version: 1, a padrão). Ela fica "congelada" em "1";
    // a versão 2 (abaixo) vale da versão 2 em diante. Veja WebConfig e OpenApiVersionGroups.
    @GetMapping(value = "/movies/{id}", version = "1")
    @Operation(summary = "Buscar filme por ID", description = "Retorna os dados de um filme, incluindo os gêneros. (Versão 1)")
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

    // VERSÃO 2 do mesmo endpoint (X-API-Version: 2). O "+" significa "da versão 2 em diante".
    // Mudanças em relação à v1: "duracao" virou "duracaoMinutos" e "generos" agora é uma lista de nomes.
    @GetMapping(value = "/movies/{id}", version = "2+")
    @Operation(summary = "Buscar filme por ID (v2)", description = "Retorna os dados de um filme. Na versão 2, a duração vem em 'duracaoMinutos' e os gêneros vêm como lista de nomes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Filme não encontrado para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieResponseV2>> getMovieV2(
            @Parameter(description = "ID numérico do filme a ser buscado", example = "1")
            @PathVariable long id) {
        // O Service é o mesmo da v1; só a forma da resposta muda.
        MovieResponseV2 movieResponse = MovieResponseV2.from(movieService.findById(id));

        EntityModel<MovieResponseV2> model = EntityModel.of(movieResponse,
                linkTo(methodOn(MovieController.class).getMovieV2(id)).withSelfRel(),
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