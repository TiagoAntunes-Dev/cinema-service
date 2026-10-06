package com.cinelog.api.Service;

import com.cinelog.api.Entity.User;
import com.cinelog.api.DTO.UserRequest;
import com.cinelog.api.DTO.UserResponse;
import com.cinelog.api.Repository.UserRepository;
import com.cinelog.api.Repository.WatchListRepository;
import com.cinelog.api.Exception.ConflictException;
import com.cinelog.api.Exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * @Service: Indica que esta classe contém a lógica de negócios.
 * O Spring cria um "Bean" dessa classe e a deixa pronta para ser injetada no Controller.
 */
@Service
public class UserService {


    // Atrinutos
    private final UserRepository userRepository;
    private final WatchListRepository watchListRepository;

    // Contructor Injection
    public UserService(UserRepository userRepository, WatchListRepository watchListRepository) {
        this.userRepository = userRepository;
        this.watchListRepository = watchListRepository;
    }

    // CREATE: Cria um novo usuário (padrão: createXxx, retorna DTO)
    public UserResponse createUser(UserRequest userRequest) {

        // Regra de negócio: não pode haver dois usuários com o mesmo e-mail.
        if (userRepository.existsByEmailIgnoreCase(userRequest.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + userRequest.email());
        }

        User user = new User(userRequest.name(), userRequest.email());
        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    // READ: Busca um usuário pelo ID
    public UserResponse getUserById(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        return toResponse(user);
    }

    // READ: Lista os usuários de forma PAGINADA (consistente com os outros services)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    // DELETE: Apaga o usuário do banco com validação de existência e de FK
    public void deleteUserId(long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + id);
        }

        // Regra de negócio: não se apaga um usuário que ainda tem watchlists (erro 409).
        if (watchListRepository.existsByUserId(id)) {
            throw new ConflictException("Não é possível excluir o usuário com ID " + id + ", pois ele possui watchlists");
        }

        userRepository.deleteById(id);
    }

    // UPDATE: Atualiza os dados de um usuário existente
    public UserResponse updateUser(long id, UserRequest userRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        // Só valida duplicidade se o e-mail realmente mudou.
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(userRequest.email());

        if (emailChanged && userRepository.existsByEmailIgnoreCase(userRequest.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + userRequest.email());
        }

        user.setName(userRequest.name());
        user.setEmail(userRequest.email());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    // Converte a entidade User no DTO de saída (DRY — centraliza a conversão)
    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}