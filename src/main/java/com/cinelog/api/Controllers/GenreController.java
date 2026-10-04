package com.cinelog.api.Controllers;

import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.GenreRequest;
import com.cinelog.api.DTO.GenreResponse;
import com.cinelog.api.Service.GenreService;
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
 * GenreController: recebe as requisições HTTP sobre gêneros e delega o trabalho ao GenreService.
 * Todas as rotas começam com "/api/v1" e usam o substantivo no plural: "/genres".
 *
 * HATEOAS: as respostas agora vêm envoltas em EntityModel (recurso único) ou PagedModel (listas paginadas).
 * Isso adiciona um bloco "_links" em cada JSON, com URLs para self, update, delete, etc.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Gêneros", description = "Endpoints responsáveis pela gestão de gêneros de filmes")
public class GenreController {

    private final GenreService genreService;

    // PagedResourcesAssembler: bean do Spring HATEOAS que transforma um Page<T> em PagedModel<T>,
    // já adicionando automaticamente os links de paginação (self, first, last, next, prev).
    private final PagedResourcesAssembler<GenreResponse> pagedAssembler;

    // Injeção de dependência via Construtor. É ativada automaticamente pelo Spring.
    public GenreController(GenreService genreService,
                           PagedResourcesAssembler<GenreResponse> pagedAssembler) {
        this.genreService = genreService;
        this.pagedAssembler = pagedAssembler;
    }

    /**
     * POST /genres: cria um gênero.
     * @Valid aciona as validações do GenreRequest; @RequestBody transforma o JSON no objeto Java.
     * A resposta vem com _links: "self" (o recurso criado) e "all-genres" (a lista completa).
     */
    @PostMapping("/genres")
    @Operation(summary = "Criar um novo gênero", description = "Valida os dados enviados no corpo e cadastra um novo gênero de filme. O nome não pode se repetir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Gênero criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (nome vazio ou com mais de 50 caracteres)",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "Já existe um gênero com esse nome",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<GenreResponse>> createGenre(@Valid @RequestBody GenreRequest genreRequest) {
        GenreResponse genreResponse = genreService.createGenre(genreRequest);

        // Envolve o DTO em um EntityModel e adiciona os links.
        // methodOn(GenreController.class).getGenre(...) gera a URL real do endpoint de busca por ID.
        EntityModel<GenreResponse> model = EntityModel.of(genreResponse,
                linkTo(methodOn(GenreController.class).getGenre(genreResponse.id())).withSelfRel(),
                linkTo(methodOn(GenreController.class).getAllGenres(null)).withRel("all-genres")
        );

        // Status 201 (Created) + cabeçalho "Location" com a URI do novo recurso + DTO com links no corpo
        return ResponseEntity
                .created(URI.create("/api/v1/genres/" + genreResponse.id()))
                .body(model);
    }

    /**
     * GET /genres/{id}: busca um gênero pelo ID. O @PathVariable extrai o "{id}" da URL.
     * A resposta traz os links: self, all-genres, update, delete e movies (navegabilidade para os filmes do gênero).
     */
    @GetMapping("/genres/{id}")
    @Operation(summary = "Buscar gênero por ID", description = "Retorna os dados de um gênero específico com base no ID informado na URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gênero encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Gênero não encontrado para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<GenreResponse>> getGenre(
            @Parameter(description = "ID numérico do gênero a ser buscado", example = "1")
            @PathVariable long id) {
        GenreResponse genreResponse = genreService.findById(id);

        // Monta o EntityModel com os links relevantes.
        // O link "movies" aponta para a rota aninhada do MovieController (navegabilidade entre recursos).
        EntityModel<GenreResponse> model = EntityModel.of(genreResponse,
                linkTo(methodOn(GenreController.class).getGenre(id)).withSelfRel(),
                linkTo(methodOn(GenreController.class).getAllGenres(null)).withRel("all-genres"),
                linkTo(methodOn(GenreController.class).updateGenre(id, null)).withRel("update"),
                linkTo(methodOn(GenreController.class).deleteGenre(id)).withRel("delete"),
                linkTo(methodOn(MovieController.class).getMoviesByGenre(id, null)).withRel("movies")
        );

        return ResponseEntity.ok(model);
    }

    /**
     * GET /genres: lista os gêneros de forma PAGINADA.
     * Pageable: o Spring monta esse objeto sozinho a partir dos parâmetros da URL.
     * Exemplo: /api/v1/genres?page=0&size=5&sort=nome,asc (a primeira página é a 0).
     * Sem parâmetros, usa o padrão do Spring: página 0 com 20 registros.
     * @ParameterObject: só faz o Swagger mostrar page, size e sort como campos separados.
     *
     * HATEOAS: a resposta é um PagedModel, que inclui os links de paginação
     * (self, first, last, next, prev) automaticamente.
     */
    @GetMapping("/genres")
    @Operation(summary = "Listar gêneros", description = "Retorna os gêneros cadastrados de forma paginada. Use os parâmetros page, size e sort para controlar a página.")
    @ApiResponse(responseCode = "200", description = "Página de gêneros retornada com sucesso")
    public ResponseEntity<PagedModel<EntityModel<GenreResponse>>> getAllGenres(
            @ParameterObject Pageable pageable) {
        Page<GenreResponse> page = genreService.findAll(pageable);

        // toModel recebe a Page e uma função que converte cada GenreResponse em EntityModel (com self link).
        PagedModel<EntityModel<GenreResponse>> model = pagedAssembler.toModel(
                page,
                genreResponse -> EntityModel.of(genreResponse,
                        linkTo(methodOn(GenreController.class).getGenre(genreResponse.id())).withSelfRel()
                )
        );

        return ResponseEntity.ok(model);
    }

    /**
     * GET /genres/search?nome=...: consulta personalizada por nome.
     * O Spring escolhe esta rota (e não a "/genres/{id}") porque "search" é um texto fixo na URL.
     * @RequestParam: pega o valor que vem depois do "?" na URL.
     */
    @GetMapping("/genres/search")
    @Operation(summary = "Buscar gêneros por nome", description = "Retorna, de forma paginada, os gêneros cujo nome contém o texto informado (sem diferenciar maiúsculas de minúsculas).")
    @ApiResponse(responseCode = "200", description = "Página de gêneros retornada com sucesso (pode vir vazia)")
    public ResponseEntity<PagedModel<EntityModel<GenreResponse>>> searchGenres(
            @Parameter(description = "Parte do nome do gênero", example = "ação")
            @RequestParam String nome,
            @ParameterObject Pageable pageable) {
        Page<GenreResponse> page = genreService.findByName(nome, pageable);

        PagedModel<EntityModel<GenreResponse>> model = pagedAssembler.toModel(
                page,
                genreResponse -> EntityModel.of(genreResponse,
                        linkTo(methodOn(GenreController.class).getGenre(genreResponse.id())).withSelfRel()
                )
        );

        return ResponseEntity.ok(model);
    }

    /**
     * PUT /genres/{id}: atualiza um gênero existente (substituição completa).
     * A resposta traz os links: self, all-genres e delete.
     */
    @PutMapping("/genres/{id}")
    @Operation(summary = "Atualizar gênero", description = "Atualiza o nome de um gênero existente. O novo nome não pode pertencer a outro gênero.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gênero atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erros de validação nos campos informados",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Gênero não encontrado para atualização",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "Já existe outro gênero com esse nome",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<GenreResponse>> updateGenre(
            @Parameter(description = "ID numérico do gênero a ser atualizado", example = "1")
            @PathVariable long id,
            @Valid @RequestBody GenreRequest genreRequest) {
        GenreResponse genreResponse = genreService.updateGenre(id, genreRequest);

        EntityModel<GenreResponse> model = EntityModel.of(genreResponse,
                linkTo(methodOn(GenreController.class).getGenre(id)).withSelfRel(),
                linkTo(methodOn(GenreController.class).getAllGenres(null)).withRel("all-genres"),
                linkTo(methodOn(GenreController.class).deleteGenre(id)).withRel("delete")
        );

        return ResponseEntity.ok(model);
    }

    /**
     * DELETE /genres/{id}: apaga um gênero.
     * Retorna 204 (No Content) — sem corpo, então não há links.
     */
    @DeleteMapping("/genres/{id}")
    @Operation(summary = "Deletar gênero", description = "Remove um gênero pelo ID. Não é possível remover um gênero que ainda está associado a filmes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Gênero deletado com sucesso (sem corpo de resposta)"),
            @ApiResponse(responseCode = "404", description = "Gênero não encontrado para deleção",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "O gênero está associado a filmes e não pode ser removido",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<Void> deleteGenre(
            @Parameter(description = "ID numérico do gênero a ser excluído", example = "2")
            @PathVariable long id) {
        genreService.deleteGenre(id);

        // Status 204 (No Content): deu certo, sem corpo na resposta
        return ResponseEntity.noContent().build();
    }
}