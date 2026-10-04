package com.cinelog.api.Repository;

import com.cinelog.api.Entity.WatchStatus;
import com.cinelog.api.Entity.WatchlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * WatchlistItemRepository: acesso ao banco para a entidade WatchlistItem.
 * Os métodos básicos e o findAll(Pageable) já vêm do JpaRepository.
 */
public interface WatchListItemRepository extends JpaRepository<WatchlistItem, Long> {

    // Itens de uma watchlist, paginados (watchlist.id = watchlistId)
    Page<WatchlistItem> findByWatchlistId(Long watchlistId, Pageable pageable);

    // Busca personalizada: itens de uma watchlist filtrados pelo status (QUERO_VER, VENDO ou VISTO)
    Page<WatchlistItem> findByWatchlistIdAndStatus(Long watchlistId, WatchStatus status, Pageable pageable);

    // Todos os itens de uma watchlist, sem paginação: usado para apagá-los antes de apagar a própria watchlist
    List<WatchlistItem> findByWatchlistId(Long watchlistId);

    // Verifica se o filme já está na watchlist (para não adicionar o mesmo filme duas vezes)
    boolean existsByWatchlistIdAndMovieId(Long watchlistId, Long movieId);

    // Verifica se o filme está em alguma watchlist (usado para impedir a exclusão de um filme que está em uso)
    boolean existsByMovieId(Long movieId);
}
