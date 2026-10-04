package com.cinelog.api.Controllers; // Define o pacote onde o Controller está localizado

import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.UserRequest;
import com.cinelog.api.DTO.UserResponse;
import com.cinelog.api.Entity.User;
import com.cinelog.api.Service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Importa os métodos estáticos do WebMvcLinkBuilder:
// linkTo(...), methodOn(...) — usados para gerar os links HATEOAS.
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * @RestController: Avisa o Spring que essa classe vai lidar com requisições da Web.
 * Todos os retornos dos métodos serão convertidos automaticamente em JSON para o cliente.
 *
 * HATEOAS: as respostas vêm envoltas em EntityModel (recurso único) ou CollectionModel (lista simples).
 * Cada usuário aponta para si mesmo, para suas watchlists e para as ações possíveis (update, delete).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Usuários", description = "Endpoints responsáveis pela gestão de usuários (Fase 1)")
public class UserController {

    // Injeção de dependência via Construtor. É ativada automaticamente pelo Spring.
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Hidden // O Swagger vai ignorar este endpoint completamente
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public String helloWorld() {
        return "Bem-vindo à API Cinema-Service! O sistema está online e operando.";
    }

    /**
     * @PostMapping("/users"): Mapeia requisições HTTP do tipo POST.
     * @Valid: Força o Spring a acionar as validações (@NotBlank, @Email) do UserRequest.
     * @RequestBody: Pega o JSON enviado no corpo da requisição e transforma no objeto Java UserRequest.
     *
     * HATEOAS: o usuário criado vem com links self, all-users e watchlists (navegabilidade).
     */
    @PostMapping("/users")
    @Operation(summary = "Criar um novo usuário", description = "Valida os dados enviados no corpo e persiste um novo usuário no banco de dados em memória.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (nome vazio ou e-mail incorreto)",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<UserResponse>> createUser(@Valid @RequestBody UserRequest userRequest) {

        // Aciona o serviço para salvar o usuário e armazena a entidade que retornou
        User user = userService.saveUser(userRequest);

        // Converte a entidade de banco (User) num DTO de resposta (UserResponse) para esconder detalhes do banco
        UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail());

        // Envolve o DTO em EntityModel e adiciona links.
        // "watchlists" é navegabilidade: do usuário, desce para suas listas.
        EntityModel<UserResponse> model = EntityModel.of(userResponse,
                linkTo(methodOn(UserController.class).getUser(userResponse.id())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"),
                linkTo(methodOn(WatchListController.class).getWatchListsByUser(userResponse.id(), null)).withRel("watchlists")
        );

        // Retorna Status 201 (Created), adiciona o cabeçalho "Location" com a URI do recurso e injeta o DTO com links no corpo.
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + user.getId()))
                .body(model);
    }

    /**
     * @GetMapping("/users/{id}"): Mapeia requisições GET para um ID específico.
     * @PathVariable: Extrai o "{id}" da URL (ex: /api/v1/users/5).
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados detalhados de um usuário específico com base no ID informado na URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<UserResponse>> getUser(
            @Parameter(description = "ID numérico do usuário a ser buscado", example = "1")
            @PathVariable long id) {

        // Busca o usuário. Se não existir, a exceção é lançada lá no Service.
        UserResponse userResponse = userService.getUserById(id);

        // Links: self, all, update, delete + navegabilidade para watchlists.
        EntityModel<UserResponse> model = EntityModel.of(userResponse,
                linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"),
                linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"),
                linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete"),
                linkTo(methodOn(WatchListController.class).getWatchListsByUser(id, null)).withRel("watchlists")
        );

        return ResponseEntity.ok(model);
    }

    /**
     * @GetMapping("/users"): Rota plural para listar todos.
     *
     * HATEOAS: CollectionModel envolve a lista e adiciona links (self, all-users).
     * Cada item também tem seus links individuais.
     */
    @GetMapping("/users")
    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista contendo todos os usuários cadastrados no sistema.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))))
    public ResponseEntity<CollectionModel<EntityModel<UserResponse>>> getAllUsers() {

        // Chama o service que devolve uma Lista pronta de DTOs
        List<UserResponse> userList = userService.getAllUsers();

        // Converte cada UserResponse em EntityModel com links individuais
        List<EntityModel<UserResponse>> userModels = userList.stream()
                .map(user -> EntityModel.of(user,
                        linkTo(methodOn(UserController.class).getUser(user.id())).withSelfRel(),
                        linkTo(methodOn(WatchListController.class).getWatchListsByUser(user.id(), null)).withRel("watchlists")
                ))
                .toList();

        // CollectionModel agrupa a lista e adiciona um link "self" para a coleção
        CollectionModel<EntityModel<UserResponse>> collection = CollectionModel.of(userModels,
                linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel()
        );

        // Retorna 200 OK com a coleção em JSON
        return ResponseEntity.ok(collection);
    }

    /**
     * @DeleteMapping: Mapeia requisições do tipo DELETE.
     */
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Deletar usuário", description = "Remove permanentemente um usuário do banco de dados através do seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário deletado com sucesso (sem corpo de resposta)"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado para deleção",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID numérico do usuário a ser excluído", example = "2")
            @PathVariable long id) {

        // Manda o service deletar pelo ID
        userService.deleteUserId(id);

        // Retorna status 204 (No Content). Deu certo, sem corpo na resposta (portanto, sem links).
        return ResponseEntity.noContent().build();
    }

    /**
     * @PutMapping: Mapeia requisições PUT (usado para atualização de substituição completa).
     * Usa o @PathVariable para achar o usuário e o @RequestBody para pegar os dados novos.
     *
     * HATEOAS: o usuário atualizado vem com os mesmos links da busca por ID.
     */
    @PutMapping("/users/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados cadastrais (nome e e-mail) de um usuário existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erros de validação nos campos informados",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado para atualização",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<EntityModel<UserResponse>> updateUser(
            @Parameter(description = "ID numérico do usuário a ser atualizado", example = "1")
            @PathVariable long id,
            @Valid @RequestBody UserRequest userRequest) {

        // Manda o service atualizar passando o ID antigo e os dados novos
        UserResponse userResponse = userService.updateUser(id, userRequest);

        // Links: self, all, delete + navegabilidade para watchlists.
        EntityModel<UserResponse> model = EntityModel.of(userResponse,
                linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"),
                linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete"),
                linkTo(methodOn(WatchListController.class).getWatchListsByUser(id, null)).withRel("watchlists")
        );

        // Retorna Status 200 (OK) com os dados novos do usuário atualizado e links HATEOAS
        return ResponseEntity.ok(model);
    }
}
