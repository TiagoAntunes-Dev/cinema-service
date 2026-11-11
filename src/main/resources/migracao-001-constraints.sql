-- ============================================================================================
-- MIGRAÇÃO 001: constraints UNIQUE que impedem duplicatas mesmo com requisições simultâneas.
--
-- Para bancos que JÁ EXISTEM (o banco.sql só roda uma vez, ao criar o banco do zero).
-- Execute no MySQL, nesta ordem, e só avance se cada passo funcionar:
--
--   mysql -u <usuario> -p cinema_db < src/main/resources/migracao-001-constraints.sql
--
-- (ou cole os comandos no cliente que você usa)
-- ============================================================================================

-- PASSO 1: procure duplicatas. As duas consultas precisam devolver ZERO linhas.
-- Se devolverem alguma, apague/una os registros repetidos antes de continuar,
-- senão o PASSO 3 falha.
SELECT nome, COUNT(*) AS total FROM genres GROUP BY nome HAVING COUNT(*) > 1;
SELECT watchlist_id, movie_id, COUNT(*) AS total
FROM watchlist_items GROUP BY watchlist_id, movie_id HAVING COUNT(*) > 1;

-- PASSO 2: veja se já existe alguma chave única nas tabelas (o Hibernate pode ter criado uma sozinha
-- com um nome do tipo UK_...). Se genres.nome já aparece como Non_unique = 0, pule o comando de genres do PASSO 3.
SHOW INDEX FROM genres;
SHOW INDEX FROM watchlist_items;

-- PASSO 3: crie as constraints.
ALTER TABLE genres
    ADD CONSTRAINT uk_genres_nome UNIQUE (nome);

ALTER TABLE watchlist_items
    ADD CONSTRAINT uk_watchlist_items_watchlist_movie UNIQUE (watchlist_id, movie_id);
