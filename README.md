# 🎬 Cinelog API

> **API REST para catalogação de filmes e gerenciamento de watchlists pessoais.**

A **Cinelog API** permite cadastrar filmes, associá-los a gêneros e armazenar informações detalhadas (sinopse, orçamento, país de origem). Cada usuário pode criar listas pessoais de filmes (*watchlists*) e acompanhar o status de cada item (**QUERO_VER / VENDO / VISTO / ABANDONADO**), com nota opcional de 1 a 10.

A API implementa **HATEOAS**, oferecendo navegabilidade entre os recursos via hiperlinks retornados em cada resposta.

---

## 📑 Índice

- [Visão geral](#-visão-geral)
- [Stack técnica](#-stack-técnica)
- [Arquitetura](#-arquitetura)
- [Recursos disponíveis](#-recursos-disponíveis)
- [Como executar](#-como-executar)
- [Documentação interativa (Swagger)](#-documentação-interativa-swagger)
- [Endpoints principais](#-endpoints-principais)
- [Paginação](#-paginação)
- [HATEOAS](#-hateoas)
- [Tratamento de erros](#-tratamento-de-erros)
- [Exemplo de fluxo completo](#-exemplo-de-fluxo-completo)
- [Estrutura do projeto](#-estrutura-do-projeto)
- [Autor](#-autor)

---

## 📖 Visão geral

A Cinelog API foi projetada para servir como back-end de uma aplicação de gerenciamento de filmes e listas pessoais. Ela cobre **6 recursos principais**:

| Recurso | Rota base | Descrição |
|---|---|---|
| **Usuários** | `/users` | Donos das watchlists |
| **Gêneros** | `/genres` | Categorias de filmes (ação, drama, ficção científica...) |
| **Filmes** | `/movies` | Catálogo principal |
| **Detalhes de Filmes** | `/movie-details` | Informações estendidas (1:1 com filme) |
| **Watchlists** | `/watchlists` | Listas pessoais de cada usuário |
| **Itens de Watchlist** | `/watchlist-items` | Filmes dentro de uma watchlist (com status e nota) |

---

## 🛠️ Stack técnica

| Camada | Tecnologia |
|---|---|
| **Linguagem** | Java 17 |
| **Framework** | Spring Boot 4.1.1 |
| **Persistência** | Spring Data JPA + Hibernate |
| **Banco de dados** | H2 (em memória) |
| **Documentação** | SpringDoc OpenAPI 3.1 (Swagger UI) |
| **Hipermídia** | Spring HATEOAS |
| **Validação** | Bean Validation (Jakarta) |
| **Build** | Maven |
| **Testes** | Spring Boot Starter Test |

---

## 🏗️ Arquitetura

A API segue **arquitetura em camadas**, com separação clara de responsabilidades:

```
┌──────────────────┐
│    Controller    │  ← Recebe requisições HTTP, delega ao Service
├──────────────────┤
│      DTO         │  ← Contratos de entrada e saída (Request / Response)
├──────────────────┤
│     Service      │  ← Regras de negócio, orquestração
├──────────────────┤
│    Repository    │  ← Acesso ao banco de dados (Spring Data JPA)
├──────────────────┤
│     Entity       │  ← Mapeamento objeto-relacional (JPA/Hibernate)
└──────────────────┘
       ↑
   Exception         ← Exceções customizadas + @RestControllerAdvice
```

**Decisões arquiteturais:**
- **DTOs** separados das entidades para não expor o modelo de banco.
- **Tratamento centralizado de erros** com `@RestControllerAdvice` (formato padronizado).
- **HATEOAS** para navegabilidade entre recursos.
- **Bean Validation** nos DTOs de entrada.
- **Paginação** em todas as listagens.

---

## 📦 Recursos disponíveis

### 👤 Usuários — `/users`
Donos das watchlists.

### 🎭 Gêneros — `/genres`
Categorias de filmes, com nome único (validação de duplicata).

### 🎬 Filmes — `/movies`
Catálogo principal. Cada filme pode ter **vários gêneros** (relacionamento muitos-para-muitos).

### 📝 Detalhes de Filmes — `/movie-details`
Informações estendidas: sinopse, orçamento, país de origem, idioma. Relacionamento **1:1** com filme.

### 📋 Watchlists — `/watchlists`
Listas pessoais criadas por cada usuário.

### 🎞️ Itens de Watchlist — `/watchlist-items`
Filmes adicionados a uma watchlist, com **status** (`QUERO_VER`, `VENDO`, `VISTO`, `ABANDONADO`) e **nota** (1 a 10).

---

## 🚀 Como executar

### Pré-requisitos

- **JDK 17+**
- **Maven** (ou use o `mvnw` incluso)

### Passos

```bash
# 1. Clone o repositório
git clone https://github.com/seu-usuario/cinelog-api.git
cd cinelog-api

# 2. Compile o projeto
./mvnw clean install

# 3. Execute a aplicação
./mvnw spring-boot:run
```

A API estará disponível em: **`http://localhost:8080`**

### Console H2

Para inspecionar o banco de dados em memória:

- **URL:** `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:mem:testdb`
- **Usuário:** `sa`
- **Senha:** *(vazio)*

> ℹ️ O banco é **em memória**. Os dados são reiniciados a cada execução e populados a partir do arquivo `data.sql`.

---

## 📚 Documentação interativa (Swagger)

A documentação completa está disponível em:

```
http://localhost:8080/swagger-ui.html
```

O Swagger UI permite **explorar e testar todos os endpoints** diretamente no navegador, com:

- Descrição detalhada da API
- Exemplos de requisições e respostas
- Todos os códigos de status HTTP possíveis
- Links HATEOAS em cada resposta

---

## 🛣️ Endpoints principais

### 👤 Usuários

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/users` | Lista todos os usuários |
| `GET` | `/api/v1/users/{id}` | Busca usuário por ID |
| `POST` | `/api/v1/users` | Cria novo usuário |
| `PUT` | `/api/v1/users/{id}` | Atualiza usuário |
| `DELETE` | `/api/v1/users/{id}` | Remove usuário |
| `GET` | `/api/v1/users/{id}/watchlists` | Watchlists do usuário |

### 🎭 Gêneros

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/genres` | Lista gêneros (paginado) |
| `GET` | `/api/v1/genres/{id}` | Busca gênero por ID |
| `GET` | `/api/v1/genres/search?nome=...` | Busca por nome (parcial) |
| `POST` | `/api/v1/genres` | Cria gênero |
| `PUT` | `/api/v1/genres/{id}` | Atualiza gênero |
| `DELETE` | `/api/v1/genres/{id}` | Remove gênero |
| `GET` | `/api/v1/genres/{id}/movies` | Filmes do gênero |

### 🎬 Filmes

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/movies` | Lista filmes (paginado) |
| `GET` | `/api/v1/movies/{id}` | Busca filme por ID |
| `GET` | `/api/v1/movies/search?titulo=...` | Busca por título |
| `POST` | `/api/v1/movies` | Cria filme |
| `PUT` | `/api/v1/movies/{id}` | Atualiza filme |
| `DELETE` | `/api/v1/movies/{id}` | Remove filme |
| `GET` | `/api/v1/movies/{id}/details` | Detalhes do filme |

### 📝 Detalhes de Filmes

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/movie-details` | Lista detalhes (paginado) |
| `GET` | `/api/v1/movie-details/{id}` | Busca detalhes por ID |
| `GET` | `/api/v1/movie-details/search?paisOrigem=...` | Busca por país |
| `POST` | `/api/v1/movie-details` | Cria detalhes |
| `PUT` | `/api/v1/movie-details/{id}` | Atualiza (substituição completa) |
| `PATCH` | `/api/v1/movie-details/{id}` | Atualiza (parcial) |
| `DELETE` | `/api/v1/movie-details/{id}` | Remove detalhes |

### 📋 Watchlists

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/watchlists` | Lista watchlists (paginado) |
| `GET` | `/api/v1/watchlists/{id}` | Busca watchlist por ID |
| `POST` | `/api/v1/watchlists` | Cria watchlist |
| `PUT` | `/api/v1/watchlists/{id}` | Atualiza watchlist |
| `DELETE` | `/api/v1/watchlists/{id}` | Remove watchlist |
| `GET` | `/api/v1/watchlists/{id}/items` | Itens da watchlist (filtro `?status=`) |

### 🎞️ Itens de Watchlist

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/watchlist-items` | Lista itens (paginado) |
| `GET` | `/api/v1/watchlist-items/{id}` | Busca item por ID |
| `POST` | `/api/v1/watchlist-items` | Adiciona filme à watchlist |
| `PUT` | `/api/v1/watchlist-items/{id}` | Atualiza item |
| `DELETE` | `/api/v1/watchlist-items/{id}` | Remove item |

---

## 📄 Paginação

Endpoints que retornam listas são **paginados**. Parâmetros aceitos:

| Parâmetro | Descrição | Padrão |
|---|---|---|
| `page` | Número da página (começa em 0) | `0` |
| `size` | Itens por página | `20` |
| `sort` | Campo e direção (`campo,asc` ou `campo,desc`) | — |

**Exemplo:**
```
GET /api/v1/movies?page=0&size=10&sort=titulo,asc
```

**Resposta (formato `PagedModel` do HATEOAS):**
```json
{
  "_embedded": {
    "movieResponseList": [ /* filmes */ ]
  },
  "page": {
    "size": 10,
    "totalElements": 4,
    "totalPages": 1,
    "number": 0
  },
  "_links": {
    "self":  { "href": ".../movies?page=0&size=10" },
    "first": { "href": "..." },
    "last":  { "href": "..." }
  }
}
```

---

## 🔗 HATEOAS

Toda resposta de **sucesso** inclui um bloco `_links` com URLs para navegação entre os recursos:

| Link | Significado |
|---|---|
| `self` | O próprio recurso |
| `all-...` | A coleção completa |
| `update` / `delete` | Ações possíveis |
| Recursos relacionados | Ex.: `movie`, `watchlist`, `items`, `details` |

**Exemplo — `GET /api/v1/movies/1`:**
```json
{
  "_links": {
    "self":       { "href": "http://localhost:8080/api/v1/movies/1" },
    "all-movies": { "href": "http://localhost:8080/api/v1/movies" },
    "update":     { "href": "http://localhost:8080/api/v1/movies/1" },
    "delete":     { "href": "http://localhost:8080/api/v1/movies/1" },
    "details":    { "href": "http://localhost:8080/api/v1/movies/1/details" }
  },
  "id": 1,
  "titulo": "Interestelar",
  "anoLancamento": 2014,
  "duracao": 169,
  "classificacaoIndicativa": "12",
  "generos": [ /* ... */ ]
}
```

Com isso, o cliente pode **descobrir dinamicamente** os recursos relacionados, sem precisar conhecer as URLs de antemão.

---

## 🚨 Tratamento de erros

Todos os erros retornam o **mesmo formato padronizado**:

```json
{
  "timestamp": "2026-10-04T14:55:57",
  "status": 404,
  "errorMessage": "NOT_FOUND",
  "message": "Filme não encontrado com o ID: 999",
  "path": "/api/v1/movies/999"
}
```

### Códigos HTTP utilizados

| Código | Significado | Quando acontece |
|---|---|---|
| `200` | OK | Requisição bem-sucedida |
| `201` | Created | Recurso criado (com header `Location`) |
| `204` | No Content | Recurso deletado (sem corpo) |
| `400` | Bad Request | Erros de validação ou regra de negócio |
| `404` | Not Found | Recurso não encontrado |
| `409` | Conflict | Duplicata ou FK em uso |
| `500` | Internal Server Error | Erro interno não previsto |

---

## 🧪 Exemplo de fluxo completo

**Cenário:** criar um filme e adicioná-lo a uma watchlist.

```bash
# 1. Criar um filme
curl -X POST http://localhost:8080/api/v1/movies \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Duna",
    "anoLancamento": 2021,
    "duracao": 155,
    "classificacaoIndicativa": "14",
    "genreIds": [1, 3]
  }'

# → 201 Created
# → Location: /api/v1/movies/4
# → _links: self, all-movies, details

# 2. Adicionar o filme a uma watchlist
curl -X POST http://localhost:8080/api/v1/watchlist-items \
  -H "Content-Type: application/json" \
  -d '{
    "watchlistId": 1,
    "movieId": 4,
    "status": "QUERO_VER"
  }'

# → 201 Created
# → _links: self, watchlist, movie

# 3. Consultar os itens da watchlist
curl http://localhost:8080/api/v1/watchlists/1/items?status=QUERO_VER

# → 200 OK (PagedModel com links HATEOAS)
```

---

## 📮 Coleção do Postman

![Coleção Postman rodando com 66 testes aprovados](https://github.com/TiagoAntunes-Dev/cinema-service/raw/master/postman/Postman-Colecao.png)

Para testar a API rapidamente, importe a coleção na pasta `postman/`...

## 📂 Estrutura do projeto

```
cinema-service/
├── src/main/java/com/cinelog/api/
│   ├── Configuration/       # Configurações (OpenAPI, HATEOAS)
│   ├── Controllers/         # Endpoints REST
│   ├── DTO/                 # Data Transfer Objects (Request/Response)
│   ├── Entity/              # Entidades JPA
│   ├── Exception/           # Exceções customizadas + handler global
│   ├── Repository/          # Interfaces Spring Data JPA
│   └── Service/             # Regras de negócio
├── src/main/resources/
│   ├── application.properties
│   └── data.sql             # Dados iniciais (seed)
└── pom.xml
```

---

## 🤝 Contribuições

Este é um **projeto acadêmico**. Sugestões e melhorias são bem-vindas via *issues* ou *pull requests*.

---

## 📄 Licença

Distribuído sob a licença **MIT**. Veja [`LICENSE`](LICENSE) para mais informações.

---

## 👨‍💻 Autor

**Tiago Antunes**

- GitHub: [@tiagoantunes](https://github.com/TiagoAntunes-Dev)
- Email: tiagoantunes1974@gmail.com

---

<p align="center">
  Feito com ☕ e Spring Boot
</p>
