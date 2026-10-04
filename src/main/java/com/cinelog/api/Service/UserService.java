package com.cinelog.api.Service;

import com.cinelog.api.Entity.User;
import com.cinelog.api.DTO.UserRequest;
import com.cinelog.api.DTO.UserResponse;
import com.cinelog.api.Repository.UserRepository;
import com.cinelog.api.Repository.WatchListRepository;
import com.cinelog.api.Exception.ConflictException;
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

    private final UserRepository userRepository;
    private final WatchListRepository watchListRepository;

    public UserService(UserRepository userRepository, WatchListRepository watchListRepository) {
        this.userRepository = userRepository;
        this.watchListRepository = watchListRepository;
    }

    // CREATE: Processa a criação de um novo usuário
    public User saveUser(UserRequest userRequest) {

        // Regra de negócio: não pode haver dois usuários com o mesmo e-mail.
        // 409 (Conflict): conflita com um recurso que já existe.
        if (userRepository.existsByEmailIgnoreCase(userRequest.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + userRequest.email());
        }

        User user = new User(userRequest.name(), userRequest.email());

        User savedUser = userRepository.save(user);

        return savedUser;
    }

    // READ: Busca um usuário específico pelo ID
    public UserResponse getUserById(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));

        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    // READ: Busca todos os usuários cadastrados
    public List<UserResponse> getAllUsers() {
        List<User> allUsers = userRepository.findAll();

        List<UserResponse> userResponseList = new ArrayList<>();

        for (User user : allUsers) {
            userResponseList.add(new UserResponse(user.getId(), user.getName(), user.getEmail()));
        }

        return userResponseList;
    }

    // DELETE: Apaga o usuário do banco com validação de existência
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
        // Sem isso, mandar o mesmo e-mail que o usuário já tem daria conflito com ele mesmo.
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(userRequest.email());

        if (emailChanged && userRepository.existsByEmailIgnoreCase(userRequest.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + userRequest.email());
        }

        user.setName(userRequest.name());
        user.setEmail(userRequest.email());

        User savedUser = userRepository.save(user);

        return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }
}