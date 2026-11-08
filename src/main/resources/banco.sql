CREATE DATABASE IF NOT EXISTS cinema_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cinema_db;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE genres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE TABLE movies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    ano_lancamento INT,
    duracao INT,
    classificacao_indicativa VARCHAR(10)
);

CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

CREATE TABLE movie_details (
    movie_id BIGINT PRIMARY KEY,
    sinopse_longa TEXT,
    orcamento DECIMAL(15, 2),
    pais_origem VARCHAR(100),
    idioma_original VARCHAR(50),
    notas_producao TEXT,
    CONSTRAINT fk_movie_details_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

CREATE TABLE watchlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_watchlists_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE watchlist_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    watchlist_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    nota INT,
    CONSTRAINT fk_watchlist_items_watchlist FOREIGN KEY (watchlist_id) REFERENCES watchlists(id) ON DELETE CASCADE,
    CONSTRAINT fk_watchlist_items_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

INSERT INTO users (name, email) VALUES ('Tiago Antunes', 'tiagoantunes1974@example.com');
INSERT INTO users (name, email) VALUES ('Carlos Souza', 'carlos.souza@example.com');
INSERT INTO users (name, email) VALUES ('Spring Boot', 'spring@boot.com');

INSERT INTO genres (nome) VALUES ('Ficção Científica');
INSERT INTO genres (nome) VALUES ('Ação');
INSERT INTO genres (nome) VALUES ('Drama');
INSERT INTO genres (nome) VALUES ('Comédia');

INSERT INTO movies (titulo, ano_lancamento, duracao, classificacao_indicativa)
VALUES ('Interestelar', 2014, 169, '12');
INSERT INTO movies (titulo, ano_lancamento, duracao, classificacao_indicativa)
VALUES ('Matrix', 1999, 136, '14');
INSERT INTO movies (titulo, ano_lancamento, duracao, classificacao_indicativa)
VALUES ('O Auto da Compadecida', 2000, 104, '12');

INSERT INTO movie_genres (movie_id, genre_id) VALUES (1, 1);
INSERT INTO movie_genres (movie_id, genre_id) VALUES (1, 3);
INSERT INTO movie_genres (movie_id, genre_id) VALUES (2, 1);
INSERT INTO movie_genres (movie_id, genre_id) VALUES (2, 2);
INSERT INTO movie_genres (movie_id, genre_id) VALUES (3, 4);

INSERT INTO movie_details (movie_id, sinopse_longa, orcamento, pais_origem, idioma_original, notas_producao)
VALUES (1,
        'Um grupo de astronautas viaja por um buraco de minhoca em busca de um novo lar para a humanidade.',
        165000000.00,
        'Estados Unidos',
        'Inglês',
        'Parte das cenas foi filmada na Islândia.');

INSERT INTO movie_details (movie_id, sinopse_longa, orcamento, pais_origem, idioma_original, notas_producao)
VALUES (2,
        'Um programador e hacker descobre que a realidade em que vive é uma simulação criada por máquinas e se junta a um grupo de rebeldes para libertar a humanidade.',
        63000000.00,
        'Estados Unidos',
        'Inglês',
        'Dirigido pelas irmãs Wachowski e filmado na Austrália, em Sydney.');

INSERT INTO movie_details (movie_id, sinopse_longa, orcamento, pais_origem, idioma_original, notas_producao)
VALUES (3,
        'Os pobres e astutos João Grilo e Chicó enfrentam patrões, o cangaço e a Igreja no sertão nordestino, até serem julgados com a ajuda da Compadecida.',
        1000000.00,
        'Brasil',
        'Português',
        'Adaptação da peça de Ariano Suassuna, dirigida por Guel Arraes e filmada em Cabaceiras, na Paraíba. Nasceu como minissérie da Globo.');

INSERT INTO watchlists (nome, descricao, user_id)
VALUES ('Para ver no fim de semana', 'Filmes de ficção científica que ainda não assisti', 1);

INSERT INTO watchlists (nome, descricao, user_id)
VALUES ('Clássicos', 'Filmes que preciso reassistir', 2);

INSERT INTO watchlist_items (watchlist_id, movie_id, status, nota)
VALUES (1, 1, 'QUERO_VER', NULL);

INSERT INTO watchlist_items (watchlist_id, movie_id, status, nota)
VALUES (1, 2, 'VISTO', 9);

INSERT INTO watchlist_items (watchlist_id, movie_id, status, nota)
VALUES (2, 3, 'VENDO', NULL);