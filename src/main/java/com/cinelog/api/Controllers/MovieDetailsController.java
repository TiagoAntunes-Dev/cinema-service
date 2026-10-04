package com.cinelog.api.Controllers;

import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.MovieDetailsPatchRequest;
import com.cinelog.api.DTO.MovieDetailsRequest;
import com.cinelog.api.DTO.MovieDetailsResponse;
import com.cinelog.api.Service.MovieDetailsService;
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

// Importa os métodos estáticos do WebMvcLinkBuilder:
// linkTo(...), methodOn(...) — usados para gerar os links HATEOAS.
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * MovieDetailsController: recebe as requisições HTTP sobre os detalhes dos filmes.
 * Rotas: "/movie-details" (CRUD completo) e "/movies/{movieId}/details" (detalhes de um filme específico).
 *
 * HATEOAS: as respostas vêm envoltas em EntityModel (recurso único) ou PagedModel (listas paginadas).
 * Cada resposta inclui um bloco "_links" com self, update, delete e navegabilidade para o filme dono.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Detalhes dos Filmes", description = "Endpoints responsáveis pelos detalhes dos filmes (sinopse, orçamento, país de origem...)")
public class MovieDetailsController {

    private final MovieDetailsService movieDetailsService;

    // PagedResourcesAssembler: bean do Spring HATEOAS que transforma um Page<T> em PagedModel<T>,
    // já adicionando automaticamente os links de paginação (self, first, last, next, prev).
    private final PagedResourcesAssembler<MovieDetailsResponse> pagedAssembler;

    public MovieDetailsController(MovieDetailsService movieDetailsService,
                                  PagedResourcesAssembler<MovieDetailsResponse> pagedAssembler) {
        this.movieDetailsService = movieDetailsService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping("/movie-details")
    @Operation(summary = "Criar detalhes de um filme", description = "Cadastra os detalhes de um filme existente. Cada filme só pode ter um registro de detalhes (relacionamento 1:1).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Detalhes criados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (movieId ausente, orçamento negativo, textos muito longos...)",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Filme informado não existe",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "O filme já possui detalhes cadastrados",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieDetailsResponse>> createMovieDetails(
            @Valid @RequestBody MovieDetailsRequest request) {
        MovieDetailsResponse response = movieDetailsService.createMovieDetails(request);

        // Envolve o DTO em EntityModel e adiciona links.
        // Navegabilidade: do registro de detalhes, sobe para o filme dono (movie).
        EntityModel<MovieDetailsResponse> model = EntityModel.of(response,
                linkTo(methodOn(MovieDetailsController.class).getMovieDetails(response.id())).withSelfRel(),
                linkTo(methodOn(MovieDetailsController.class).getAllMovieDetails(null)).withRel("all-movie-details"),
                linkTo(methodOn(MovieController.class).getMovie(response.movieId())).withRel("movie")
        );

        // Status 201 (Created) + cabeçalho "Location" com a URI do novo recurso + DTO com links no corpo
        return ResponseEntity
                .created(URI.create("/api/v1/movie-details/" + response.id()))
                .body(model);
    }

    @GetMapping("/movie-details/{id}")
    @Operation(summary = "Buscar detalhes por ID", description = "Retorna um registro de detalhes pelo ID do próprio registro.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhes encontrados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Detalhes não encontrados para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieDetailsResponse>> getMovieDetails(
            @Parameter(description = "ID numérico do registro de detalhes", example = "1")
            @PathVariable long id) {
        MovieDetailsResponse response = movieDetailsService.findById(id);

        // Navegabilidade: "movie" leva ao filme dono; "by-movie" leva à rota aninhada.
        EntityModel<MovieDetailsResponse> model = EntityModel.of(response,
                linkTo(methodOn(MovieDetailsController.class).getMovieDetails(id)).withSelfRel(),
                linkTo(methodOn(MovieDetailsController.class).getAllMovieDetails(null)).withRel("all-movie-details"),
                linkTo(methodOn(MovieDetailsController.class).updateMovieDetails(id, null)).withRel("update"),
                linkTo(methodOn(MovieDetailsController.class).patchMovieDetails(id, null)).withRel("patch"),
                linkTo(methodOn(MovieDetailsController.class).deleteMovieDetails(id)).withRel("delete"),
                linkTo(methodOn(MovieController.class).getMovie(response.movieId())).withRel("movie")
        );

        return ResponseEntity.ok(model);
    }

    @GetMapping("/movie-details")
    @Operation(summary = "Listar detalhes de filmes", description = "Retorna os registros de detalhes de forma paginada. Use os parâmetros page, size e sort.")
    @ApiResponse(responseCode = "200", description = "Página de detalhes retornada com sucesso")
    public ResponseEntity<PagedModel<EntityModel<MovieDetailsResponse>>> getAllMovieDetails(
            @ParameterObject Pageable pageable) {
        Page<MovieDetailsResponse> page = movieDetailsService.findAll(pageable);

        PagedModel<EntityModel<MovieDetailsResponse>> model = pagedAssembler.toModel(
                page,
                details -> EntityModel.of(details,
                        linkTo(methodOn(MovieDetailsController.class).getMovieDetails(details.id())).withSelfRel(),
                        linkTo(methodOn(MovieController.class).getMovie(details.movieId())).withRel("movie")
                )
        );

        return ResponseEntity.ok(model);
    }

    // Consulta personalizada 1: busca por país de origem
    @GetMapping("/movie-details/search")
    @Operation(summary = "Buscar detalhes por país de origem", description = "Retorna, de forma paginada, os detalhes de filmes cujo país de origem contém o texto informado.")
    @ApiResponse(responseCode = "200", description = "Página de detalhes retornada com sucesso (pode vir vazia)")
    public ResponseEntity<PagedModel<EntityModel<MovieDetailsResponse>>> searchMovieDetails(
            @Parameter(description = "Parte do nome do país de origem", example = "Estados")
            @RequestParam String paisOrigem,
            @ParameterObject Pageable pageable) {
        Page<MovieDetailsResponse> page = movieDetailsService.findByCountry(paisOrigem, pageable);

        PagedModel<EntityModel<MovieDetailsResponse>> model = pagedAssembler.toModel(
                page,
                details -> EntityModel.of(details,
                        linkTo(methodOn(MovieDetailsController.class).getMovieDetails(details.id())).withSelfRel(),
                        linkTo(methodOn(MovieController.class).getMovie(details.movieId())).withRel("movie")
                )
        );

        return ResponseEntity.ok(model);
    }

    // Consulta personalizada 2: rota aninhada. Lê-se "os detalhes do filme X": /movies/1/details
    @GetMapping("/movies/{movieId}/details")
    @Operation(summary = "Buscar detalhes de um filme", description = "Retorna os detalhes do filme informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhes encontrados com sucesso"),
            @ApiResponse(responseCode = "404", description = "O filme não possui detalhes cadastrados",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieDetailsResponse>> getMovieDetailsByMovie(
            @Parameter(description = "ID numérico do filme", example = "1")
            @PathVariable long movieId) {
        MovieDetailsResponse response = movieDetailsService.findByMovieId(movieId);

        // Links: "self" aponta para a rota aninhada (não para /movie-details/{id}),
        // reforçando a navegabilidade pai → filho.
        EntityModel<MovieDetailsResponse> model = EntityModel.of(response,
                linkTo(methodOn(MovieDetailsController.class).getMovieDetailsByMovie(movieId)).withSelfRel(),
                linkTo(methodOn(MovieDetailsController.class).getMovieDetails(response.id())).withRel("by-id"),
                linkTo(methodOn(MovieController.class).getMovie(movieId)).withRel("movie")
        );

        return ResponseEntity.ok(model);
    }

    @PutMapping("/movie-details/{id}")
    @Operation(summary = "Atualizar detalhes", description = "Atualiza os dados de um registro de detalhes. O movieId enviado precisa ser o mesmo do registro.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhes atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erros de validação ou tentativa de trocar o filme dos detalhes",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Detalhes não encontrados para atualização",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieDetailsResponse>> updateMovieDetails(
            @Parameter(description = "ID numérico do registro de detalhes", example = "1")
            @PathVariable long id,
            @Valid @RequestBody MovieDetailsRequest request) {
        MovieDetailsResponse response = movieDetailsService.updateMovieDetails(id, request);

        EntityModel<MovieDetailsResponse> model = EntityModel.of(response,
                linkTo(methodOn(MovieDetailsController.class).getMovieDetails(id)).withSelfRel(),
                linkTo(methodOn(MovieDetailsController.class).patchMovieDetails(id, null)).withRel("patch"),
                linkTo(methodOn(MovieDetailsController.class).deleteMovieDetails(id)).withRel("delete"),
                linkTo(methodOn(MovieController.class).getMovie(response.movieId())).withRel("movie")
        );

        return ResponseEntity.ok(model);
    }

    @PatchMapping("/movie-details/{id}")
    @Operation(
            summary = "Atualizar parcialmente detalhes",
            description = "Atualiza SÓ os campos enviados no corpo. Campos omitidos mantêm o valor atual. " +
                    "O vínculo com o filme não pode ser alterado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhes atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erros de validação nos campos informados",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Detalhes não encontrados para atualização",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<MovieDetailsResponse>> patchMovieDetails(
            @Parameter(description = "ID numérico do registro de detalhes", example = "1")
            @PathVariable long id,
            @Valid @RequestBody MovieDetailsPatchRequest request) {
        MovieDetailsResponse response = movieDetailsService.patchMovieDetails(id, request);

        EntityModel<MovieDetailsResponse> model = EntityModel.of(response,
                linkTo(methodOn(MovieDetailsController.class).getMovieDetails(id)).withSelfRel(),
                linkTo(methodOn(MovieDetailsController.class).updateMovieDetails(id, null)).withRel("update"),
                linkTo(methodOn(MovieDetailsController.class).deleteMovieDetails(id)).withRel("delete"),
                linkTo(methodOn(MovieController.class).getMovie(response.movieId())).withRel("movie")
        );

        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/movie-details/{id}")
    @Operation(summary = "Deletar detalhes", description = "Remove um registro de detalhes. O filme continua cadastrado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Detalhes deletados com sucesso (sem corpo de resposta)"),
            @ApiResponse(responseCode = "404", description = "Detalhes não encontrados para deleção",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<Void> deleteMovieDetails(
            @Parameter(description = "ID numérico do registro de detalhes", example = "1")
            @PathVariable long id) {
        movieDetailsService.deleteMovieDetails(id);

        // Status 204 (No Content): deu certo, sem corpo na resposta (portanto, sem links)
        return ResponseEntity.noContent().build();
    }
}