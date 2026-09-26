package com.cinelog.api.Service; // Define o pacote da camada intermediária que abriga as regras de negócio

import com.cinelog.api.Entity.User;
import com.cinelog.api.Models.UserRequest;
import com.cinelog.api.Models.UserResponse;
import com.cinelog.api.Repository.UserRepository;
import com.cinelog.api.Exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Service: Indica que esta classe contém a lógica de negócios.
 * O Spring cria um "Bean" dessa classe e a deixa pronta para ser injetada no Controller.
 */
@Service
public class UserService {

    private UserRepository userRepository;

    /**
     * Injeção de dependência via Construtor (Recomendado).
     * O Spring injeta o UserRepository automaticamente ao instanciar o UserService.
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // CREATE: Processa a criação de um novo usuário
    public User saveUser(UserRequest userRequest) {
        // CORRIGIDO: Ordem ajustada para (name, email) correspondendo exatamente à entidade User
        User user = new User(userRequest.name(), userRequest.email());

        // O JPA gera o SQL INSERT, salva no banco e devolve a entidade preenchida com o ID autogerado
        User savedUser = userRepository.save(user);

        return savedUser;
    }

    // READ: Busca um usuário específico pelo ID
    public UserResponse getUserById(long id) {
        // findById devolve um "Optional". Se existir, pega o usuário.
        // Se não existir, lança a nossa exceção customizada de erro 404 (que cai no GlobalExceptionHandler).
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        // Mapeia a Entidade de volta para um DTO de Resposta antes de devolver para o Controller
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    // READ: Busca todos os usuários cadastrados
    public List<UserResponse> getAllUsers() {
        // Busca todas as entidades do banco
        List<User> allUsers = userRepository.findAll();

        // Cria uma lista vazia de DTOs
        List<UserResponse> userResponseList = new ArrayList<>();

        // Laço (foreach): Para cada entidade (User) da lista de banco, transforma em DTO e adiciona na lista nova
        for (User user : allUsers) {
            userResponseList.add(new UserResponse(user.getId(), user.getName(), user.getEmail()));
        }

        return userResponseList;
    }

    // DELETE: Apaga o usuário do banco com validação de existência
    public void deleteUserId(long id) {
        // CORRIGIDO: Verifica se o ID realmente existe antes de mandar apagar.
        // Se não existir, lança o erro 404 controlado pelo GlobalExceptionHandler (evitando o erro 500).
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + id);
        }

        // O JPA apaga diretamente do banco pelo ID
        userRepository.deleteById(id);
    }

    // UPDATE: Atualiza os dados de um usuário existente
    public UserResponse updateUser(long id, UserRequest userRequest) {
        // Primeiro verifica se o usuário existe. Se não existir, lança erro 404 e interrompe a execução aqui.
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        // Se chegou aqui, o usuário existe. Então atualizamos os campos em memória com os dados novos do DTO.
        user.setName(userRequest.name());
        user.setEmail(userRequest.email());

        // Ao chamar save() passando uma entidade que JÁ TEM UM ID definido, o JPA executa um UPDATE em vez de um INSERT
        User savedUser = userRepository.save(user);

        // Devolvemos o resultado formatado como DTO
        return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }
}