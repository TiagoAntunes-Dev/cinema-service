package com.cinelog.api.Controllers; // Define o pacote onde o Controller está localizado

import com.cinelog.api.Entity.User;
import com.cinelog.api.DTO.APIError;
import com.cinelog.api.DTO.UserRequest;
import com.cinelog.api.DTO.UserResponse;
import com.cinelog.api.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * @RestController: Avisa o Spring que essa classe vai lidar com requisições da Web.
 * Todos os retornos dos métodos serão convertidos automaticamente em JSON para o cliente.
 */
@RestController
/**
 * @RequestMapping: Agrupa e padroniza a rota raiz deste controller.
 * Todas as rotas abaixo começarão obrigatoriamente com "/api/v1".
 */

@RequestMapping("/api/v1")
@Tag(name = "Usuários", description = "Endpoints responsáveis pela gestão de usuários (Fase 1)")

public class UserController {

    // Declaração da dependência da camada de serviço (onde estão as regras de negócio)
    // private? Para garantir o encapsulamento. Apenas o próprio UserController precisa ter acesso direto ao Service.
    private UserService userService;

    // Injeção de dependência via Construtor. É ativada automaticamente pelo Spring.
    public UserController(UserService userService) {

        this.userService = userService;
    }

    /**
     * @PostMapping("/users"): Mapeia requisições HTTP do tipo POST.
     * @Valid: Força o Spring a acionar as validações (@NotBlank, @Email) do UserRequest.
     * @RequestBody: Pega o JSON enviado no corpo da requisição e transforma no objeto Java UserRequest.
     */
    @PostMapping("/users")
    @Operation(summary = "Criar um novo usuário", description = "Valida os dados enviados no corpo e persiste um novo usuário no banco de dados em memória.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (nome vazio ou e-mail incorreto)",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })

    // User Response: É o seu DTO de Saída. É o objeto limpo que você retorna para o cliente
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest){
        // Aciona o serviço para salvar o usuário e armazena a entidade que retornou
        User user = userService.saveUser(userRequest);

        // Converte a entidade de banco (User) num DTO de resposta (UserResponse) para esconder detalhes do banco
        UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail());

        // Retorna Status 201 (Created), adiciona o cabeçalho "Location" com a URI do recurso e injeta o DTO no corpo.
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + user.getId())) // 1. Status 201 + Header Location
                .body(userResponse);                                  // 2. Corpos dos dados em JSON
    }

    /**
     * @GetMapping("/users/{id}"): Mapeia requisições GET para um ID específico.
     * @PathVariable: Extrai o "{id}" da URL (ex: /api/v1/users/5).
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados detalhados de um usuário específico com base no ID informado na URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado para o ID especificado",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "ID numérico do usuário a ser buscado", example = "1")
            @PathVariable long id){
        // Busca o usuário. Se não existir, a exceção é lançada lá no Service.
        UserResponse userResponse = userService.getUserById(id);

        // Retorna Status 200 (OK) com o DTO no corpo da resposta
        return ResponseEntity.ok(userResponse);
    }

    /**
     * @GetMapping("/users"): Rota plural para listar todos.
     */
    @GetMapping("/users")
    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista contendo todos os usuários cadastrados no sistema.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))))
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        // Chama o service que devolve uma Lista pronta de DTOs
        List<UserResponse> userList =  userService.getAllUsers();

        // Retorna 200 OK com o Array JSON contendo todos os usuários
        return ResponseEntity.ok(userList);
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
            @PathVariable long id){
        // Manda o service deletar pelo ID
        userService.deleteUserId(id);

        // Retorna status 204 (No Content). Padrão REST para quando uma ação dá certo mas não devolve nenhum corpo (JSON).
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * @PutMapping: Mapeia requisições PUT (usado para atualização de substituição completa).
     * Usa o @PathVariable para achar o usuário e o @RequestBody para pegar os dados novos.
     */
    @PutMapping("/users/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados cadastrais (nome e e-mail) de um usuário existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erros de validação nos campos informados",
                    content = @Content(schema = @Schema(implementation = APIError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado para atualização",
                    content = @Content(schema = @Schema(implementation = APIError.class)))
    })
    ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID numérico do usuário a ser atualizado", example = "1")
            @PathVariable long id,
            @Valid @RequestBody UserRequest userRequest) {

        // Manda o service atualizar passando o ID antigo e os dados novos
        UserResponse userResponse = userService.updateUser(id, userRequest);

        // Retorna Status 200 (OK) com os dados novos do usuário atualizado.
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }
}
