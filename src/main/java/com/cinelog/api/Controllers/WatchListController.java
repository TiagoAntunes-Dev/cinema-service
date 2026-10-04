package com.cinelog.api.Controllers;

import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.WatchListRequest;
import com.cinelog.api.DTO.WatchListResponse;
import com.cinelog.api.Service.WatchListService;
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
 * WatchListController: recebe as requisições HTTP sobre watchlists.
 * Rotas: "/watchlists" e "/users/{userId}/watchlists" (as listas de um usuário).
 *
 * HATEOAS: as respostas vêm envoltas em EntityModel (recurso único) ou PagedModel (listas paginadas).
 * Cada watchlist aponta para seu dono ("user") e para seus itens ("items") — navegabilidade entre recursos.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Watchlists", description = "Endpoints responsáveis pela gestão das watchlists (listas de filmes dos usuários)")
public class WatchListController {

    private final WatchListService watchListService;

    // PagedResourcesAssembler: bean do Spring HATEOAS que transforma um Page<T> em PagedModel<T>,
    // já adicionando automaticamente os links de paginação (self, first, last, next, prev).
    private final PagedResourcesAssembler<WatchListResponse> pagedAssembler;

    public WatchListController(WatchListService watchListService,
                               PagedResourcesAssembler<WatchListResponse> pagedAssembler) {
        this.watchListService = watchListService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping("/watchlists")
    @Operation(summary = "Criar uma nova watchlist", description = "Cria uma watchlist para um usuário existente (informado pelo userId).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Watchlist criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (nome vazio ou fora de 3 a 100 caracteres, userId ausente...)",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário informado não existe",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<WatchListResponse>> createWatchList(
            @Valid @RequestBody WatchListRequest request) {
        WatchListResponse response = watchListService.createWatchList(request);

        // Envolve o DTO em EntityModel e adiciona links.
        // Navegabilidade: "user" leva ao dono; "items" leva aos itens da lista.
        EntityModel<WatchListResponse> model = EntityModel.of(response,
                linkTo(methodOn(WatchListController.class).getWatchList(response.id())).withSelfRel(),
                linkTo(methodOn(WatchListController.class).getAllWatchLists(null)).withRel("all-watchlists"),
                linkTo(methodOn(WatchListController.class).getWatchListsByUser(response.userId(), null)).withRel("by-user"),
                linkTo(methodOn(WatchListItemController.class).getItemsByWatchList(response.id(), null, null)).withRel("items"),
                linkTo(methodOn(UserController.class).getUser(response.userId())).withRel("user")
        );

        return ResponseEntity
                .created(URI.create("/api/v1/watchlists/" + response.id()))
                .body(model);
    }

    @GetMapping("/watchlists/{id}")
    @Operation(summary = "Buscar watchlist por ID", description = "Retorna os dados de uma watchlist específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Watchlist encontrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Watchlist não encontrada para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<WatchListResponse>> getWatchList(
            @Parameter(description = "ID numérico da watchlist", example = "1")
            @PathVariable long id) {
        WatchListResponse response = watchListService.findById(id);

        // Todos os links relevantes: self, all, update, delete, by-user, items, user.
        EntityModel<WatchListResponse> model = EntityModel.of(response,
                linkTo(methodOn(WatchListController.class).getWatchList(id)).withSelfRel(),
                linkTo(methodOn(WatchListController.class).getAllWatchLists(null)).withRel("all-watchlists"),
                linkTo(methodOn(WatchListController.class).updateWatchList(id, null)).withRel("update"),
                linkTo(methodOn(WatchListController.class).deleteWatchList(id)).withRel("delete"),
                linkTo(methodOn(WatchListController.class).getWatchListsByUser(response.userId(), null)).withRel("by-user"),
                linkTo(methodOn(WatchListItemController.class).getItemsByWatchList(id, null, null)).withRel("items"),
                linkTo(methodOn(UserController.class).getUser(response.userId())).withRel("user")
        );

        return ResponseEntity.ok(model);
    }

    @GetMapping("/watchlists")
    @Operation(summary = "Listar watchlists", description = "Retorna as watchlists cadastradas de forma paginada. Use os parâmetros page, size e sort.")
    @ApiResponse(responseCode = "200", description = "Página de watchlists retornada com sucesso")
    public ResponseEntity<PagedModel<EntityModel<WatchListResponse>>> getAllWatchLists(
            @ParameterObject Pageable pageable) {
        Page<WatchListResponse> page = watchListService.findAll(pageable);

        PagedModel<EntityModel<WatchListResponse>> model = pagedAssembler.toModel(
                page,
                watchList -> EntityModel.of(watchList,
                        linkTo(methodOn(WatchListController.class).getWatchList(watchList.id())).withSelfRel(),
                        linkTo(methodOn(WatchListItemController.class).getItemsByWatchList(watchList.id(), null, null)).withRel("items"),
                        linkTo(methodOn(UserController.class).getUser(watchList.userId())).withRel("user")
                )
        );

        return ResponseEntity.ok(model);
    }

    // Consulta personalizada: rota aninhada. Lê-se "as watchlists do usuário X": /users/1/watchlists
    @GetMapping("/users/{userId}/watchlists")
    @Operation(summary = "Listar watchlists de um usuário", description = "Retorna, de forma paginada, as watchlists que pertencem ao usuário informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de watchlists retornada com sucesso (pode vir vazia)"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<PagedModel<EntityModel<WatchListResponse>>> getWatchListsByUser(
            @Parameter(description = "ID numérico do usuário", example = "1")
            @PathVariable long userId,
            @ParameterObject Pageable pageable) {
        Page<WatchListResponse> page = watchListService.findByUser(userId, pageable);

        PagedModel<EntityModel<WatchListResponse>> model = pagedAssembler.toModel(
                page,
                watchList -> EntityModel.of(watchList,
                        linkTo(methodOn(WatchListController.class).getWatchList(watchList.id())).withSelfRel(),
                        linkTo(methodOn(WatchListItemController.class).getItemsByWatchList(watchList.id(), null, null)).withRel("items"),
                        linkTo(methodOn(UserController.class).getUser(userId)).withRel("user")
                )
        );

        return ResponseEntity.ok(model);
    }

    @PutMapping("/watchlists/{id}")
    @Operation(summary = "Atualizar watchlist", description = "Atualiza o nome e a descrição de uma watchlist. O userId enviado precisa ser o do dono atual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Watchlist atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erros de validação ou tentativa de trocar o dono da lista",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Watchlist não encontrada para atualização",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<WatchListResponse>> updateWatchList(
            @Parameter(description = "ID numérico da watchlist", example = "1")
            @PathVariable long id,
            @Valid @RequestBody WatchListRequest request) {
        WatchListResponse response = watchListService.updateWatchList(id, request);

        EntityModel<WatchListResponse> model = EntityModel.of(response,
                linkTo(methodOn(WatchListController.class).getWatchList(id)).withSelfRel(),
                linkTo(methodOn(WatchListController.class).getAllWatchLists(null)).withRel("all-watchlists"),
                linkTo(methodOn(WatchListController.class).deleteWatchList(id)).withRel("delete"),
                linkTo(methodOn(WatchListItemController.class).getItemsByWatchList(id, null, null)).withRel("items"),
                linkTo(methodOn(UserController.class).getUser(response.userId())).withRel("user")
        );

        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/watchlists/{id}")
    @Operation(summary = "Deletar watchlist", description = "Remove uma watchlist e todos os itens dela.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Watchlist deletada com sucesso (sem corpo de resposta)"),
            @ApiResponse(responseCode = "404", description = "Watchlist não encontrada para deleção",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<Void> deleteWatchList(
            @Parameter(description = "ID numérico da watchlist", example = "1")
            @PathVariable long id) {
        watchListService.deleteWatchList(id);

        // Status 204 (No Content): deu certo, sem corpo na resposta (portanto, sem links)
        return ResponseEntity.noContent().build();
    }
}
