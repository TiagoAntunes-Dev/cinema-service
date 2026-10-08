package com.cinelog.api.Controllers;

import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.UserRequest;
import com.cinelog.api.DTO.UserResponse;
import com.cinelog.api.Service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Usuários", description = "Endpoints responsáveis pela gestão de usuários")
public class UserController {

    private final UserService userService;
    private final PagedResourcesAssembler<UserResponse> pagedAssembler;

    public UserController(UserService userService,
                          PagedResourcesAssembler<UserResponse> pagedAssembler) {
        this.userService = userService;
        this.pagedAssembler = pagedAssembler;
    }

    @Hidden
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public String helloWorld() {
        return "Bem-vindo à API Cinema-Service! O sistema está online e operando.";
    }

    @PostMapping("/users")
    @Operation(summary = "Criar um novo usuário", description = "Valida os dados enviados no corpo e persiste um novo usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<UserResponse>> createUser(@Valid @RequestBody UserRequest userRequest) {

        // ✅ Agora o service já retorna o DTO pronto
        UserResponse userResponse = userService.createUser(userRequest);

        EntityModel<UserResponse> model = EntityModel.of(userResponse,
                linkTo(methodOn(UserController.class).getUser(userResponse.id())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers(null)).withRel("all-users"),

                // Usuário pode acessar suas Watchlist com um clique
                linkTo(methodOn(WatchListController.class).getWatchListsByUser(userResponse.id(), null)).withRel("watchlists")
        );

        return ResponseEntity
                .created(URI.create("/api/v1/users/" + userResponse.id()))
                .body(model);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados de um usuário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<UserResponse>> getUser(
            @Parameter(description = "ID numérico do usuário", example = "1")
            @PathVariable long id) {
        UserResponse userResponse = userService.getUserById(id);

        EntityModel<UserResponse> model = EntityModel.of(userResponse,
                linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers(null)).withRel("all-users"),
                linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"),
                linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete"),

                // Usuário pode acessar suas Watchlist com um clique
                linkTo(methodOn(WatchListController.class).getWatchListsByUser(id, null)).withRel("watchlists")
        );

        return ResponseEntity.ok(model);
    }

    // ✅ Agora paginado, com PagedModel (consistente com os outros recursos)
    @GetMapping("/users")
    @Operation(summary = "Listar usuários", description = "Retorna os usuários cadastrados de forma paginada.")
    @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso")
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> getAllUsers(
            @ParameterObject Pageable pageable) {
        Page<UserResponse> page = userService.getAllUsers(pageable);

        PagedModel<EntityModel<UserResponse>> model = pagedAssembler.toModel(
                page,
                userResponse -> EntityModel.of(userResponse,
                        linkTo(methodOn(UserController.class).getUser(userResponse.id())).withSelfRel(),

                        // Usuário pode acessar suas Watchlist com um clique
                        linkTo(methodOn(WatchListController.class).getWatchListsByUser(userResponse.id(), null)).withRel("watchlists")
                )
        );

        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Deletar usuário", description = "Remove permanentemente um usuário do banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "Usuário possui watchlists",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID numérico do usuário", example = "2")
            @PathVariable long id) {
        userService.deleteUserId(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erros de validação",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<UserResponse>> updateUser(
            @Parameter(description = "ID numérico do usuário", example = "1")
            @PathVariable long id,
            @Valid @RequestBody UserRequest userRequest) {
        UserResponse userResponse = userService.updateUser(id, userRequest);

        EntityModel<UserResponse> model = EntityModel.of(userResponse,
                linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers(null)).withRel("all-users"),
                linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete"),

                // Usuário pode acessar suas Watchlist com um clique
                linkTo(methodOn(WatchListController.class).getWatchListsByUser(id, null)).withRel("watchlists")
        );

        return ResponseEntity.ok(model);
    }
}