package com.cinelog.api.Service;

import com.cinelog.api.DTO.WatchListItemRequest;
import com.cinelog.api.DTO.WatchListItemResponse;
import com.cinelog.api.Entity.Movie;
import com.cinelog.api.Entity.WatchList;
import com.cinelog.api.Entity.WatchStatus;
import com.cinelog.api.Entity.WatchlistItem;
import com.cinelog.api.Exception.BusinessException;
import com.cinelog.api.Exception.ConflictException;
import com.cinelog.api.Exception.ResourceNotFoundException;
import com.cinelog.api.Repository.MovieRepository;
import com.cinelog.api.Repository.WatchListItemRepository;
import com.cinelog.api.Repository.WatchListRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * WatchlistItemService: regras de negócio dos itens (um filme dentro de uma watchlist, com status e nota).
 */
@Service
public class WatchListItemService {

    private final WatchListItemRepository watchlistItemRepository;
    private final WatchListRepository watchListRepository;
    private final MovieRepository movieRepository;

    public WatchListItemService(WatchListItemRepository watchlistItemRepository,
                                WatchListRepository watchListRepository,
                                MovieRepository movieRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.watchListRepository = watchListRepository;
        this.movieRepository = movieRepository;
    }

    // CREATE: Adiciona um filme a uma watchlist
    public WatchListItemResponse createWatchlistItem(WatchListItemRequest request) {

        WatchList watchList = watchListRepository.findById(request.watchlistId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist não encontrada com o ID: " + request.watchlistId()));

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado com o ID: " + request.movieId()));

        if (watchlistItemRepository.existsByWatchlistIdAndMovieId(request.watchlistId(), request.movieId())) {
            throw new ConflictException("O filme com ID " + request.movieId() + " já está na watchlist com ID " + request.watchlistId());
        }

        // Regra: a nota só faz sentido para um filme já assistido (erro 400)
        validateRating(request.status(), request.nota());

        WatchlistItem item = new WatchlistItem(watchList, movie);
        item.setStatus(request.status());
        item.setNota(request.nota());

        WatchlistItem savedItem = watchlistItemRepository.save(item);

        return toResponse(savedItem);
    }

    // READ: Busca um item pelo ID
    public WatchListItemResponse findById(long id) {
        WatchlistItem item = watchlistItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item da watchlist não encontrado com o ID: " + id));

        return toResponse(item);
    }

    // READ: Lista todos os itens de forma paginada
    public Page<WatchListItemResponse> findAll(Pageable pageable) {
        Page<WatchlistItem> items = watchlistItemRepository.findAll(pageable);

        return items.map(item -> toResponse(item));
    }

    // READ (consulta personalizada): itens de uma watchlist, com filtro opcional por status
    public Page<WatchListItemResponse> findByWatchList(long watchlistId, WatchStatus status, Pageable pageable) {
        if (!watchListRepository.existsById(watchlistId)) {
            throw new ResourceNotFoundException("Watchlist não encontrada com o ID: " + watchlistId);
        }

        Page<WatchlistItem> items;

        if (status == null) {
            items = watchlistItemRepository.findByWatchlistId(watchlistId, pageable);
        } else {
            items = watchlistItemRepository.findByWatchlistIdAndStatus(watchlistId, status, pageable);
        }

        return items.map(item -> toResponse(item));
    }

    // UPDATE: Atualiza o status e a nota de um item
    public WatchListItemResponse updateWatchlistItem(long id, WatchListItemRequest request) {
        WatchlistItem item = watchlistItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item da watchlist não encontrado com o ID: " + id));

        // Regra: o item não "muda de lista" nem "troca de filme". Os ids enviados precisam ser os mesmos (erro 400)
        if (!item.getWatchlist().getId().equals(request.watchlistId())
                || !item.getMovie().getId().equals(request.movieId())) {
            throw new BusinessException("Não é permitido trocar a watchlist nem o filme do item. Envie os mesmos ids do registro");
        }

        validateRating(request.status(), request.nota());

        item.setStatus(request.status());
        item.setNota(request.nota());

        WatchlistItem savedItem = watchlistItemRepository.save(item);

        return toResponse(savedItem);
    }

    // DELETE: Remove um filme da watchlist (o filme e a lista continuam existindo)
    public void deleteWatchlistItem(long id) {
        if (!watchlistItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item da watchlist não encontrado com o ID: " + id);
        }

        watchlistItemRepository.deleteById(id);
    }

    // Regra de negócio: só se pode dar nota a um filme com status VISTO
    private void validateRating(WatchStatus status, Integer nota) {
        if (nota != null && status != WatchStatus.VISTO) {
            throw new BusinessException("A nota só pode ser informada quando o status for VISTO");
        }
    }

    // Converte a entidade WatchlistItem no DTO de saída
    private WatchListItemResponse toResponse(WatchlistItem item) {
        return new WatchListItemResponse(
                item.getId(),
                item.getWatchlist().getId(),
                item.getMovie().getId(),
                item.getMovie().getTitulo(),
                item.getStatus(),
                item.getNota());
    }
}
