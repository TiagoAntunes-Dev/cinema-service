package com.cinelog.api.Service;

import com.cinelog.api.DTO.WatchListRequest;
import com.cinelog.api.DTO.WatchListResponse;
import com.cinelog.api.Entity.User;
import com.cinelog.api.Entity.WatchList;
import com.cinelog.api.Entity.WatchlistItem;
import com.cinelog.api.Exception.BusinessException;
import com.cinelog.api.Exception.ResourceNotFoundException;
import com.cinelog.api.Repository.UserRepository;
import com.cinelog.api.Repository.WatchListItemRepository;
import com.cinelog.api.Repository.WatchListRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * WatchListService: regras de negócio das watchlists (listas de filmes de um usuário).
 */
@Service
public class WatchListService {

    // Campos final: imutáveis após a construção (boa prática + consistência com os Controllers)
    private final WatchListRepository watchListRepository;
    private final UserRepository userRepository;
    private final WatchListItemRepository watchlistItemRepository;

    // Injeção via construtor: o Spring injeta as três dependências automaticamente.
    public WatchListService(WatchListRepository watchListRepository,
                            UserRepository userRepository,
                            WatchListItemRepository watchlistItemRepository) {
        this.watchListRepository = watchListRepository;
        this.userRepository = userRepository;
        this.watchlistItemRepository = watchlistItemRepository;
    }

    // CREATE: Cria uma watchlist para um usuário
    public WatchListResponse createWatchList(WatchListRequest request) {

        // O usuário dono da lista precisa existir (erro 404)
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com o ID: " + request.userId()));

        WatchList watchList = new WatchList(request.nome(), request.descricao(), user);

        WatchList savedWatchList = watchListRepository.save(watchList);

        return toResponse(savedWatchList);
    }

    // READ: Busca uma watchlist pelo ID
    public WatchListResponse findById(long id) {
        WatchList watchList = watchListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Watchlist não encontrada com o ID: " + id));

        return toResponse(watchList);
    }

    // READ: Lista todas as watchlists de forma paginada
    public Page<WatchListResponse> findAll(Pageable pageable) {
        // .map(this::toResponse) é o jeito idiomático de transformar cada entidade em DTO
        return watchListRepository.findAll(pageable)
                .map(this::toResponse);
    }

    // READ (consulta personalizada): watchlists de um usuário
    public Page<WatchListResponse> findByUser(long userId, Pageable pageable) {
        // Se o usuário não existe, é erro 404 (e não uma lista vazia)
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + userId);
        }

        return watchListRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    // UPDATE: Atualiza nome e descrição de uma watchlist
    public WatchListResponse updateWatchList(long id, WatchListRequest request) {
        WatchList watchList = watchListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Watchlist não encontrada com o ID: " + id));

        // Regra: a lista não "troca de dono".
        // O userId enviado precisa ser o mesmo do registro (erro 400 / 422).
        // BusinessException é tratada pelo @RestControllerAdvice, que devolve 400.
        if (!watchList.getUser().getId().equals(request.userId())) {
            throw new BusinessException(
                    "Não é permitido trocar o usuário dono da watchlist. " +
                            "Envie o mesmo userId do registro.");
        }

        watchList.setNome(request.nome());
        watchList.setDescricao(request.descricao());

        WatchList savedWatchList = watchListRepository.save(watchList);

        return toResponse(savedWatchList);
    }

    // DELETE: Apaga uma watchlist e os itens dela.
    // @Transactional: apagar os itens e apagar a lista acontecem juntos ou nenhum acontece.
    @Transactional
    public void deleteWatchList(long id) {
        // Buscar a entidade (e não só existsById) evita uma query extra:
        // 1 query em vez de "exists" + "deleteById".
        WatchList watchList = watchListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Watchlist não encontrada com o ID: " + id));

        // Os itens guardam a chave estrangeira da lista, então precisam ser apagados ANTES da lista.
        // Se a entidade WatchList já mapear cascade = CascadeType.ALL + orphanRemoval = true,
        // este bloco pode ser removido (o delete da lista já apaga os itens).
        List<WatchlistItem> items = watchlistItemRepository.findByWatchlistId(id);
        watchlistItemRepository.deleteAll(items);

        watchListRepository.delete(watchList);
    }

    // Converte a entidade WatchList no DTO de saída.
    // Referenciado com this::toResponse nos métodos findAll e findByUser.
    private WatchListResponse toResponse(WatchList watchList) {
        return new WatchListResponse(
                watchList.getId(),
                watchList.getNome(),
                watchList.getDescricao(),
                watchList.getUser().getId());
    }
}
