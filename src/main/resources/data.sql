-- ============================================================
-- USERS
-- ============================================================
INSERT INTO users (name, email) VALUES ('Tiago Antunes', 'tiagoantunes1974@example.com');
INSERT INTO users (name, email) VALUES ('Carlos Souza', 'carlos.souza@example.com');
INSERT INTO users (name, email) VALUES ('Spring Boot', 'spring@boot.com');

-- ============================================================
-- GENRES
-- ============================================================
INSERT INTO genres (nome) VALUES ('Ficção Científica');
INSERT INTO genres (nome) VALUES ('Ação');
INSERT INTO genres (nome) VALUES ('Drama');
INSERT INTO genres (nome) VALUES ('Comédia');

-- ============================================================
-- MOVIES  (você já tem os 3)
-- ============================================================
INSERT INTO movies (titulo, ano_lancamento, duracao, classificacao_indicativa)
VALUES ('Interestelar', 2014, 169, '12');
INSERT INTO movies (titulo, ano_lancamento, duracao, classificacao_indicativa)
VALUES ('Matrix', 1999, 136, '14');
INSERT INTO movies (titulo, ano_lancamento, duracao, classificacao_indicativa)
VALUES ('O Auto da Compadecida', 2000, 104, '12');

-- ============================================================
-- MOVIE_GENRES (tabela de junção do @ManyToMany)
-- ============================================================
-- Interestelar (id=1) → Ficção Científica (id=1), Drama (id=3)
INSERT INTO movie_genres (movie_id, genre_id) VALUES (1, 1);
INSERT INTO movie_genres (movie_id, genre_id) VALUES (1, 3);

-- Matrix (id=2) → Ficção Científica (id=1), Ação (id=2)
INSERT INTO movie_genres (movie_id, genre_id) VALUES (2, 1);
INSERT INTO movie_genres (movie_id, genre_id) VALUES (2, 2);

-- O Auto da Compadecida (id=3) → Comédia (id=4)
INSERT INTO movie_genres (movie_id, genre_id) VALUES (3, 4);

-- ============================================================
-- MOVIE_DETAILS
-- ============================================================
INSERT INTO movie_details (movie_id, sinopse_longa, orcamento, pais_origem, idioma_original, notas_producao)
VALUES (1,
        'Um grupo de astronautas viaja por um buraco de minhoca em busca de um novo lar para a humanidade.',
        165000000.00,
        'Estados Unidos',
        'Inglês',
        'Parte das cenas foi filmada na Islândia.');

-- Matrix (movie_id = 2)
INSERT INTO movie_details (movie_id, sinopse_longa, orcamento, pais_origem, idioma_original, notas_producao)
VALUES (2,
        'Um programador e hacker descobre que a realidade em que vive é uma simulação criada por máquinas e se junta a um grupo de rebeldes para libertar a humanidade.',
        63000000.00,
        'Estados Unidos',
        'Inglês',
        'Dirigido pelas irmãs Wachowski e filmado na Austrália, em Sydney.');

-- O Auto da Compadecida (movie_id = 3)
INSERT INTO movie_details (movie_id, sinopse_longa, orcamento, pais_origem, idioma_original, notas_producao)
VALUES (3,
        'Os pobres e astutos João Grilo e Chicó enfrentam patrões, o cangaço e a Igreja no sertão nordestino, até serem julgados com a ajuda da Compadecida.',
        1000000,
        'Brasil',
        'Português',
        'Adaptação da peça de Ariano Suassuna, dirigida por Guel Arraes e filmada em Cabaceiras, na Paraíba. Nasceu como minissérie da Globo.');

-- ============================================================
-- WATCHLISTS
-- ============================================================
INSERT INTO watchlists (nome, descricao, user_id)
VALUES ('Para ver no fim de semana', 'Filmes de ficção científica que ainda não assisti', 1);

INSERT INTO watchlists (nome, descricao, user_id)
VALUES ('Clássicos', 'Filmes que preciso reassistir', 2);

-- ============================================================
-- WATCHLIST_ITEMS
-- ============================================================
INSERT INTO watchlist_items (watchlist_id, movie_id, status, nota)
VALUES (1, 1, 'QUERO_VER', NULL);

INSERT INTO watchlist_items (watchlist_id, movie_id, status, nota)
VALUES (1, 2, 'VISTO', 9);

INSERT INTO watchlist_items (watchlist_id, movie_id, status, nota)
VALUES (2, 3, 'VENDO', NULL);