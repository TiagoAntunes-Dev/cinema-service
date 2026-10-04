package com.cinelog.api.Repository;

import com.cinelog.api.Entity.WatchList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * WatchlistRepository: acesso ao banco para a entidade Watchlist.
 * Os métodos básicos e o findAll(Pageable) já vêm do JpaRepository.
 */
public interface WatchListRepository extends JpaRepository<WatchList, Long> {

    // Busca personalizada: watchlists de um usuário. "User" é o campo da Watchlist e "Id" é o id do usuário (user.id)
    Page<WatchList> findByUserId(Long userId, Pageable pageable);

    // Verifica se o usuário tem alguma watchlist (usado para impedir a exclusão de um usuário que ainda tem listas)
    boolean existsByUserId(Long userId);
}