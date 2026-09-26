-- ============================================================================================
-- ARQUIVO: data.sql (Script de Povoamento / Seed do Banco de Dados)
-- ============================================================================================
-- Este arquivo é executado automaticamente pelo Spring Boot ao iniciar a aplicação (graças
-- àquela configuração 'defer-datasource-initialization=true' que colocamos no application.properties).
-- Ele serve para "injetar" dados de teste no banco H2 em memória toda vez que você roda o projeto,
-- poupando o trabalho de ter que cadastrar usuários manualmente via Postman ou Swagger.

-- COMANDO 1:
-- INSERT INTO: Comando padrão da linguagem SQL para adicionar uma nova linha em uma tabela.
-- users: É o nome exato da tabela que o Hibernate criou (baseado no seu @Table(name = "users") na Entidade).
-- (name, email): São as colunas onde vamos inserir os dados.
-- PERGUNTA DE DEFESA: "Por que você não colocou o ID aqui?"
-- RESPOSTA: "Porque eu configurei o @Id da minha entidade com GenerationType.IDENTITY.
-- Isso delega para o banco de dados a responsabilidade de gerar e incrementar os IDs automaticamente."
-- VALUES: Define os valores que vão preencher as colunas (na exata mesma ordem: primeiro o nome, depois o email).
INSERT INTO users ( name, email) VALUES ( 'Tiago Antunes', 'tiagoantunes1974@example.com');

-- COMANDO 2:
-- Insere o segundo usuário. Como o banco de dados está gerenciando os IDs,
-- o Carlos receberá automaticamente o ID 2.
INSERT INTO users ( name, email) VALUES ( 'Carlos Souza', 'carlos.souza@example.com');

-- COMANDO 3:
-- Insere o terceiro usuário, que receberá o ID 3.
-- As aspas simples (' ') são obrigatórias na linguagem SQL para representar textos (Strings/Varchars).
INSERT INTO users ( name, email) VALUES ( 'Spring Boot', 'spring@boot.com');