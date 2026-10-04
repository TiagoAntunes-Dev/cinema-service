package com.cinelog.api.Controllers;

import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.WatchListItemRequest;
import com.cinelog.api.DTO.WatchListItemResponse;
import com.cinelog.api.Entity.WatchStatus;
import com.cinelog.api.Service.WatchListItemService;
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
 * WatchlistItemController: recebe as requisições HTTP sobre os itens das watchlists.
 * Rotas: "/watchlist-items" (CRUD) e "/watchlists/{watchlistId}/items" (os itens de uma lista).
 *
 * HATEOAS: cada item aponta para os DOIS recursos relacionados — a watchlist dona ("watchlist")
 * e o filme ("movie") — além dos links básicos (self, update, delete).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Itens da Watchlist", description = "Endpoints responsáveis pelos filmes dentro de cada watchlist (status e nota)")
public class WatchListItemController {

    // Campo final: imutável após a construção (consistência com os outros controllers)
    private final WatchListItemService watchlistItemService;

    // PagedResourcesAssembler: bean do Spring HATEOAS que transforma um Page<T> em PagedModel<T>,
    // já adicionando automaticamente os links de paginação (self, first, last, next, prev).
    private final PagedResourcesAssembler<WatchListItemResponse> pagedAssembler;

    // Injeção via construtor: o Spring injeta automaticamente.
    public WatchListItemController(WatchListItemService watchlistItemService,
                                   PagedResourcesAssembler<WatchListItemResponse> pagedAssembler) {
        this.watchlistItemService = watchlistItemService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping("/watchlist-items")
    @Operation(summary = "Adicionar filme a uma watchlist", description = "Cria um item ligando um filme a uma watchlist, com status e nota opcional. A nota só é aceita com o status VISTO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ids ou status ausentes, nota fora de 1 a 10, nota com status diferente de VISTO...)",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Watchlist ou filme informado não existe",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "O filme já está nesta watchlist",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<WatchListItemResponse>> createWatchlistItem(
            @Valid @RequestBody WatchListItemRequest request) {
        WatchListItemResponse response = watchlistItemService.createWatchlistItem(request);

        // Navegabilidade DUPLA: "watchlist" (a lista) e "movie" (o filme).
        EntityModel<WatchListItemResponse> model = EntityModel.of(response,
                linkTo(methodOn(WatchListItemController.class).getWatchlistItem(response.id())).withSelfRel(),
                linkTo(methodOn(WatchListItemController.class).getAllWatchlistItems(null)).withRel("all-watchlist-items"),
                linkTo(methodOn(WatchListController.class).getWatchList(response.watchlistId())).withRel("watchlist"),
                linkTo(methodOn(MovieController.class).getMovie(response.movieId())).withRel("movie")
        );

        return ResponseEntity
                .created(URI.create("/api/v1/watchlist-items/" + response.id()))
                .body(model);
    }

    @GetMapping("/watchlist-items/{id}")
    @Operation(summary = "Buscar item por ID", description = "Retorna os dados de um item da watchlist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<WatchListItemResponse>> getWatchlistItem(
            @Parameter(description = "ID numérico do item", example = "1")
            @PathVariable long id) {
        WatchListItemResponse response = watchlistItemService.findById(id);

        // Todos os links relevantes, incluindo navegação para os dois recursos relacionados.
        EntityModel<WatchListItemResponse> model = EntityModel.of(response,
                linkTo(methodOn(WatchListItemController.class).getWatchlistItem(id)).withSelfRel(),
                linkTo(methodOn(WatchListItemController.class).getAllWatchlistItems(null)).withRel("all-watchlist-items"),
                linkTo(methodOn(WatchListItemController.class).updateWatchlistItem(id, null)).withRel("update"),
                linkTo(methodOn(WatchListItemController.class).deleteWatchlistItem(id)).withRel("delete"),
                linkTo(methodOn(WatchListController.class).getWatchList(response.watchlistId())).withRel("watchlist"),
                linkTo(methodOn(MovieController.class).getMovie(response.movieId())).withRel("movie")
        );

        return ResponseEntity.ok(model);
    }

    @GetMapping("/watchlist-items")
    @Operation(summary = "Listar itens de watchlists", description = "Retorna todos os itens de forma paginada. Use os parâmetros page, size e sort.")
    @ApiResponse(responseCode = "200", description = "Página de itens retornada com sucesso")
    public ResponseEntity<PagedModel<EntityModel<WatchListItemResponse>>> getAllWatchlistItems(
            @ParameterObject Pageable pageable) {
        Page<WatchListItemResponse> page = watchlistItemService.findAll(pageable);

        PagedModel<EntityModel<WatchListItemResponse>> model = pagedAssembler.toModel(
                page,
                item -> EntityModel.of(item,
                        linkTo(methodOn(WatchListItemController.class).getWatchlistItem(item.id())).withSelfRel(),
                        linkTo(methodOn(WatchListController.class).getWatchList(item.watchlistId())).withRel("watchlist"),
                        linkTo(methodOn(MovieController.class).getMovie(item.movieId())).withRel("movie")
                )
        );

        return ResponseEntity.ok(model);
    }

    // Consulta personalizada: rota aninhada. Lê-se "os itens da watchlist X": /watchlists/1/items
    // required = false: o parâmetro "status" é opcional. Sem ele, devolve todos os itens da lista.
    @GetMapping("/watchlists/{watchlistId}/items")
    @Operation(summary = "Listar itens de uma watchlist", description = "Retorna, de forma paginada, os itens da watchlist informada. Com o parâmetro status (QUERO_VER, VENDO ou VISTO), filtra pelos itens naquele status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de itens retornada com sucesso (pode vir vazia)"),
            @ApiResponse(responseCode = "404", description = "Watchlist não encontrada para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<PagedModel<EntityModel<WatchListItemResponse>>> getItemsByWatchList(
            @Parameter(description = "ID numérico da watchlist", example = "1")
            @PathVariable long watchlistId,
            @Parameter(description = "Filtro opcional pelo status do item", example = "VISTO")
            @RequestParam(required = false) WatchStatus status,
            @ParameterObject Pageable pageable) {
        Page<WatchListItemResponse> page = watchlistItemService.findByWatchList(watchlistId, status, pageable);

        PagedModel<EntityModel<WatchListItemResponse>> model = pagedAssembler.toModel(
                page,
                item -> EntityModel.of(item,
                        linkTo(methodOn(WatchListItemController.class).getWatchlistItem(item.id())).withSelfRel(),
                        linkTo(methodOn(WatchListController.class).getWatchList(watchlistId)).withRel("watchlist"),
                        linkTo(methodOn(MovieController.class).getMovie(item.movieId())).withRel("movie")
                )
        );

        return ResponseEntity.ok(model);
    }

    @PutMapping("/watchlist-items/{id}")
    @Operation(summary = "Atualizar item", description = "Atualiza o status e a nota de um item. Os ids da watchlist e do filme enviados precisam ser os mesmos do registro.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erros de validação, nota com status diferente de VISTO ou tentativa de trocar a lista/filme",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Item não encontrado para atualização",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<WatchListItemResponse>> updateWatchlistItem(
            @Parameter(description = "ID numérico do item", example = "1")
            @PathVariable long id,
            @Valid @RequestBody WatchListItemRequest request) {
        WatchListItemResponse response = watchlistItemService.updateWatchlistItem(id, request);

        EntityModel<WatchListItemResponse> model = EntityModel.of(response,
                linkTo(methodOn(WatchListItemController.class).getWatchlistItem(id)).withSelfRel(),
                linkTo(methodOn(WatchListItemController.class).getAllWatchlistItems(null)).withRel("all-watchlist-items"),
                linkTo(methodOn(WatchListItemController.class).deleteWatchlistItem(id)).withRel("delete"),
                linkTo(methodOn(WatchListController.class).getWatchList(response.watchlistId())).withRel("watchlist"),
                linkTo(methodOn(MovieController.class).getMovie(response.movieId())).withRel("movie")
        );

        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/watchlist-items/{id}")
    @Operation(summary = "Remover filme da watchlist", description = "Remove um item da watchlist. O filme e a lista continuam cadastrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item removido com sucesso (sem corpo de resposta)"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado para deleção",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<Void> deleteWatchlistItem(
            @Parameter(description = "ID numérico do item", example = "1")
            @PathVariable long id) {
        watchlistItemService.deleteWatchlistItem(id);

        // Status 204 (No Content): deu certo, sem corpo na resposta (portanto, sem links)
        return ResponseEntity.noContent().build();
    }
}