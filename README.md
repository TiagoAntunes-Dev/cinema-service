# 🎬 Cinelog API

## Documentação da API

A Cinelog fornece uma API REST para **catalogar filmes** e **montar watchlists pessoais**. Com ela é possível cadastrar filmes, ligá-los a gêneros, guardar detalhes (sinopse, orçamento, país de origem) e acompanhar o que cada usuário quer ver, está vendo, já viu ou abandonou, com nota de 1 a 10.

As respostas seguem o estilo **HATEOAS**: cada recurso traz os links que o cliente pode seguir a partir dele.

| | |
|---|---|
| 🌐 **Produção** | <https://cinema-service-sv1b.onrender.com> |
| 📚 **Documentação interativa (Swagger)** | <https://cinema-service-sv1b.onrender.com/swagger-ui/index.html> |
| 💻 **Local** | `http://localhost:8080` |

## Índice

- [Detalhes do endpoint](#detalhes-do-endpoint)
- [Primeiros passos](#primeiros-passos)
- [Cabeçalhos](#cabeçalhos)
- [Versionamento](#versionamento)
- [Autenticação](#autenticação)
- [Limite de requisições](#limite-de-requisições)
- [Idempotência](#idempotência)
- [CORS](#cors)
- [Objeto List (paginação)](#objeto-list-paginação)
- [Links HATEOAS](#links-hateoas)
- [Objeto Error](#objeto-error)
- [Objetos da API](#objetos-da-api)
- [Referência de endpoints](#referência-de-endpoints)
- [Regras de negócio](#regras-de-negócio)
- [Executando o projeto](#executando-o-projeto)
- [Arquitetura e estrutura](#arquitetura-e-estrutura)
- [Testes e coleção do Postman](#testes-e-coleção-do-postman)

---

## Detalhes do endpoint

A API está disponível em `https://cinema-service-sv1b.onrender.com`, e todas as rotas ficam sob o prefixo `/api`.

- O corpo das requisições e das respostas é **JSON** com codificação **UTF-8**. Muitos campos contêm acentos (`"Ficção Científica"`).
- As rotas usam **substantivos no plural**: `/users`, `/genres`, `/movies`, `/movie-details`, `/watchlists` e `/watchlist-items`.
- A **versão** da API vai no cabeçalho `X-API-Version`, e **não na URL**. Veja [Versionamento](#versionamento).
- O banco de dados é **MySQL**. Os dados persistem entre reinicializações da aplicação.
- `GET /api` devolve uma mensagem de boas-vindas e serve como verificação rápida de que o serviço está no ar.
- A documentação interativa fica em `/swagger-ui/index.html`, com um seletor para alternar entre a **versão 1** e a **versão 2**, e com tema claro e escuro (botão **Alternar Tema**).
- As definições OpenAPI ficam em `/v3/api-docs/v1` e `/v3/api-docs/v2`.

## Primeiros passos

**Versão 1** (padrão, aberta, sem chave de API):

```bash
curl https://cinema-service-sv1b.onrender.com/api/movies?page=0&size=5 \
  -H "Accept: application/json"
```

**Versão 2** (exige chave de API, aplica limite de requisições e aceita idempotência):

```bash
curl -X POST https://cinema-service-sv1b.onrender.com/api/genres \
  -H "Content-Type: application/json" \
  -H "X-API-Version: 2" \
  -H "X-API-Key: cinelog-demo-key-123" \
  -H "X-Idempotency-Key: 7c9e6679-7425-40de-944b-e07fc1f90ae7" \
  -d '{"nome": "Cyberpunk"}'
```

> `cinelog-demo-key-123` é a chave de demonstração. As chaves aceitas são configuradas em `api-key.valid-keys` (veja [Configuração](#configuração)).

**Fluxo recomendado para testar do zero:**

1. Crie um gênero: `POST /api/genres`
2. Crie um filme com `genreIds`: `POST /api/movies`
3. Adicione os detalhes do filme: `POST /api/movie-details`
4. Use um dos usuários de exemplo (ids `1`, `2` e `3`) ou crie outro: `POST /api/users`
5. Crie uma watchlist para o usuário: `POST /api/watchlists`
6. Adicione o filme à lista: `POST /api/watchlist-items`

## Cabeçalhos

### Cabeçalhos de requisição

| Cabeçalho | Obrigatório | Descrição |
|---|---|---|
| `Content-Type` | Em `POST`, `PUT` e `PATCH` | Deve ser `application/json`. Qualquer outro formato retorna `415`. |
| `Accept` | Não | `application/json` ou `*/*`. Um formato que a API não produz retorna `406`. |
| `X-API-Version` | Não | Versão da API. Padrão: `1`. Veja [Versionamento](#versionamento). |
| `X-API-Key` | **Sim, na versão 2** | Chave de API. Sem ela, a resposta é `401`. |
| `X-Idempotency-Key` | Não (só nos `POST` da versão 2) | Texto único (ex.: um UUID, máximo de 255 caracteres). Veja [Idempotência](#idempotência). |
| `Origin` | Só em navegadores | Necessário para o CORS. Veja [CORS](#cors). |

### Cabeçalhos de resposta

| Cabeçalho | Quando aparece | Descrição |
|---|---|---|
| `Location` | `201 Created` | URL do recurso criado. |
| `X-RateLimit-Limit` | Toda resposta da versão 2 | Tamanho máximo do balde de requisições. |
| `X-RateLimit-Remaining` | Toda resposta da versão 2 | Requisições que ainda restam (`0` quando bloqueado). |
| `Retry-After` | `429` | Segundos de espera antes de tentar de novo. |
| `WWW-Authenticate` | `401` | `ApiKey realm="Cinelog API"`. |
| `Idempotent-Replayed` | Repetição de um `POST` idempotente | Vale `true` quando a resposta foi reaproveitada. |
| `Allow` | `405` | Métodos HTTP aceitos pela rota. |

## Versionamento

A versão é escolhida **a cada requisição**, pelo cabeçalho `X-API-Version`. A URL de um recurso nunca muda quando a API evolui.

| Requisição | Resultado |
|---|---|
| Sem o cabeçalho | Versão `1` (padrão) |
| `X-API-Version: 1` | Versão 1 |
| `X-API-Version: 2`, `2.0` ou `v2` | Versão 2 |
| Valor sem número (ex.: `abc`) | Versão 1 |
| Número maior que `2` | Tratado com as regras da versão 2 (a API não valida se essa versão existe) |

| Recurso | Versão 1 (simples) | Versão 2 (completa) |
|---|---|---|
| Chave de API (`X-API-Key`) | Não exige | **Obrigatória** (`401`) |
| Limite de requisições | Sem limite | Por chave de API (`429` + `Retry-After`) |
| Idempotência (`X-Idempotency-Key`) | Ignora o cabeçalho | Nos `POST` (`422` se o corpo mudar) |
| Representação de filme | `GET /api/movies/{id}` | `GET /api/movies/v2/{id}` |

### Representação de filme na versão 2

A versão 2 do filme tem uma rota própria: `GET /api/movies/v2/{id}`. Nela a duração vem em `duracaoMinutos` e os gêneros vêm como lista de nomes, em vez de objetos. `GET /api/movies/{id}` continua devolvendo o formato da versão 1.

```jsonc
// GET /api/movies/1  (formato v1)
{
  "id": 1,
  "titulo": "Interestelar",
  "anoLancamento": 2014,
  "duracao": 169,
  "classificacaoIndicativa": "12",
  "generos": [
    { "id": 1, "nome": "Ficção Científica" },
    { "id": 3, "nome": "Drama" }
  ]
}

// GET /api/movies/v2/1  (formato v2)
{
  "id": 1,
  "titulo": "Interestelar",
  "anoLancamento": 2014,
  "duracaoMinutos": 169,
  "classificacaoIndicativa": "12",
  "generos": ["Ficção Científica", "Drama"]
}
```

## Autenticação

Na **versão 2**, toda requisição precisa do cabeçalho `X-API-Key` com uma chave cadastrada.

| Situação | Resultado |
|---|---|
| Chave válida | A requisição segue. A chave identifica o cliente no [limite de requisições](#limite-de-requisições). |
| Cabeçalho ausente | `401 Unauthorized` |
| Chave inválida | `401 Unauthorized` |
| Requisição `OPTIONS` (preflight do CORS) | Passa sem chave |

- As chaves são comparadas em **tempo constante**, para não revelar quantos caracteres estavam certos.
- Se nenhuma chave estiver configurada, **nenhuma** requisição da versão 2 passa (a API falha "fechada").
- No Swagger, clique em **Authorize** na tela da versão 2 e informe a chave uma única vez.

## Limite de requisições

Vale **somente na versão 2**. A API usa o algoritmo **Token Bucket** (biblioteca Bucket4j):

- Cada cliente (identificado pela chave de API) tem o seu próprio **balde**, que começa cheio.
- Cada requisição **gasta 1 token**. O balde é reabastecido aos poucos, e não de uma vez só.
- Com o balde vazio, a API responde `429 Too Many Requests` com o cabeçalho `Retry-After`.
- Requisições `OPTIONS` não gastam tokens.

| Configuração | Padrão | Significado |
|---|---|---|
| `rate-limit.capacity` | `100` | Tamanho do balde (máximo de requisições em rajada) |
| `rate-limit.refill-tokens` | `100` | Tokens devolvidos a cada período |
| `rate-limit.refill-seconds` | `60` | Duração do período, em segundos |

```http
HTTP/1.1 429
Retry-After: 1
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
```

```json
{
  "timestamp": "2026-11-10T12:13:03",
  "status": 429,
  "errorMessage": "TOO_MANY_REQUESTS",
  "message": "Limite de requisições excedido. Tente novamente em 1 segundo(s).",
  "path": "/api/genres"
}
```

> Se você receber `429`, **respeite o `Retry-After`** antes de tentar de novo.

## Idempotência

`GET`, `PUT` e `DELETE` já são idempotentes pelo próprio HTTP. O `POST` não é: se o cliente reenviar (clique duplo, retry após timeout), o registro seria criado duas vezes.

Na **versão 2**, todos os `POST` aceitam o cabeçalho opcional `X-Idempotency-Key`:

| Situação | Resultado |
|---|---|
| Chave nova | Executa normalmente e **guarda a resposta por 24 horas** |
| Mesma chave e **mesmo corpo** | Não cria nada. Devolve a resposta original (`201`, `Location` e corpo) com `Idempotent-Replayed: true` |
| Mesma chave e **corpo diferente** | `422 Unprocessable Entity` |
| Sem o cabeçalho | Comportamento normal |

- Duas requisições simultâneas com a mesma chave são serializadas: a segunda espera a primeira e recebe a mesma resposta.
- Se a primeira tentativa falhar (validação, `404`, `409`...), nada é guardado e o cliente pode tentar de novo com a mesma chave.
- As respostas ficam **em memória** e somem quando a aplicação reinicia.

## CORS

O CORS permite que **front-ends em outra origem** chamem a API pelo navegador. Para receber os cabeçalhos CORS, a requisição precisa incluir o cabeçalho `Origin`, que deve corresponder à origem da página.

| Item | Configuração |
|---|---|
| **Origens permitidas** | As de `cors.allowed-origins`: `http://localhost:3000`, `http://localhost:5173`, `http://localhost:4200` e o próprio domínio do Render. **Não** usa `*` |
| **Métodos** | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| **Cabeçalhos aceitos** | `Content-Type`, `Accept`, `X-API-Key`, `X-Idempotency-Key`, `X-API-Version` |
| **Cabeçalhos legíveis pelo front-end** | `Location`, `Retry-After`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Idempotent-Replayed` |
| **Credenciais (cookies)** | Desativadas |
| **Cache do preflight** | 1 hora |

Uma origem não autorizada recebe `403` (`Invalid CORS request`). Para liberar outro front-end:

```properties
cors.allowed-origins=http://localhost:3000,https://meu-front.com
```

> O CORS é uma proteção **do navegador**. Postman, curl e o Swagger UI (que roda na mesma origem da API) não são afetados.

## Objeto List (paginação)

As listagens são **paginadas** e devolvem um `PagedModel` do HATEOAS.

| Parâmetro | Descrição | Padrão |
|---|---|---|
| `page` | Número da página (a primeira é a `0`) | `0` |
| `size` | Itens por página (**máximo `100`**) | `20` |
| `sort` | Campo e direção: `campo,asc` ou `campo,desc` | Sem ordenação |

```
GET /api/movies?page=0&size=10&sort=titulo,asc
```

| Propriedade | Tipo | Detalhes |
|---|---|---|
| `_embedded` | Objeto | Contém a lista de itens, em uma chave que depende do recurso (`movieResponseList`, `genreResponseList`...). **Se a página estiver vazia, a chave `_embedded` pode não aparecer.** |
| `page.size` | Inteiro | Itens por página |
| `page.totalElements` | Inteiro | Total de itens em todas as páginas |
| `page.totalPages` | Inteiro | Total de páginas |
| `page.number` | Inteiro | Número da página atual |
| `_links` | Objeto | `self`, `first`, `next`, `prev` e `last` (os dois do meio só existem quando há página seguinte ou anterior) |

```json
{
  "_embedded": {
    "movieResponseList": [
      {
        "id": 1,
        "titulo": "Interestelar",
        "anoLancamento": 2014,
        "duracao": 169,
        "classificacaoIndicativa": "12",
        "generos": [{ "id": 1, "nome": "Ficção Científica" }],
        "_links": { "self": { "href": "https://cinema-service-sv1b.onrender.com/api/movies/1" } }
      }
    ]
  },
  "_links": {
    "self": { "href": "https://cinema-service-sv1b.onrender.com/api/movies?page=0&size=20" }
  },
  "page": { "size": 20, "totalElements": 3, "totalPages": 1, "number": 0 }
}
```

## Links HATEOAS

Respostas de sucesso com corpo incluem `_links` com as URLs que o cliente pode seguir:

| Link | Significado |
|---|---|
| `self` | O próprio recurso |
| `all-...` | A coleção completa (ex.: `all-movies`) |
| `update` / `delete` | Ações possíveis sobre o recurso |
| Relacionados | Ex.: `details`, `movie`, `watchlist`, `items` |

```json
{
  "id": 1,
  "titulo": "Interestelar",
  "_links": {
    "self":       { "href": "https://cinema-service-sv1b.onrender.com/api/movies/1" },
    "all-movies": { "href": "https://cinema-service-sv1b.onrender.com/api/movies" },
    "update":     { "href": "https://cinema-service-sv1b.onrender.com/api/movies/1" },
    "delete":     { "href": "https://cinema-service-sv1b.onrender.com/api/movies/1" },
    "details":    { "href": "https://cinema-service-sv1b.onrender.com/api/movies/1/details" }
  }
}
```

## Objeto Error

Um objeto **Error** representa uma falha ao encontrar informação ou ao entender a entrada enviada. Todos os erros usam o mesmo formato e vêm com o código HTTP 4XX ou 5XX correspondente.

| Propriedade | Tipo | Detalhes |
|---|---|---|
| `timestamp` | Data e hora | Momento em que o erro ocorreu (ex.: `2026-11-10T12:13:03`) |
| `status` | Inteiro | Código de status HTTP |
| `errorMessage` | String | Nome do status HTTP (`NOT_FOUND`, `BAD_REQUEST`...) |
| `message` | String | Explicação do erro, legível por humanos |
| `path` | String | Rota em que o erro aconteceu |
| `errors` | Array de strings | **Só nos erros de validação (`400`).** Um item por problema, no formato `campo: mensagem` |

```json
{
  "timestamp": "2026-11-10T12:13:03",
  "status": 404,
  "errorMessage": "NOT_FOUND",
  "message": "Filme não encontrado com o ID: 999",
  "path": "/api/movies/999"
}
```

Na validação, **todos** os campos inválidos vêm de uma vez:

```json
{
  "timestamp": "2026-11-10T12:13:03",
  "status": 400,
  "errorMessage": "BAD_REQUEST",
  "message": "Erro de validação: 2 problema(s) encontrado(s). Veja os detalhes em 'errors'.",
  "path": "/api/users",
  "errors": [
    "email: O email deve ser um endereço válido",
    "name: O nome não pode estar em branco"
  ]
}
```

### Códigos de status

| Código | Significado | Quando acontece |
|---|---|---|
| `200` | OK | Consulta ou atualização bem-sucedida |
| `201` | Created | Recurso criado (o `Location` traz a URL dele) |
| `204` | No Content | Recurso removido, sem corpo |
| `400` | Bad Request | Validação, JSON mal formado, parâmetro com tipo errado ou ausente, `X-Idempotency-Key` com mais de 255 caracteres, regra de negócio |
| `401` | Unauthorized | Chave de API ausente ou inválida (versão 2) |
| `403` | Forbidden | Origem não autorizada no CORS |
| `404` | Not Found | Recurso ou rota não encontrados |
| `405` | Method Not Allowed | Método HTTP não aceito pela rota (o `Allow` informa os aceitos) |
| `406` | Not Acceptable | `Accept` pede um formato que a API não produz |
| `409` | Conflict | Duplicata ou registro ainda em uso |
| `415` | Unsupported Media Type | Corpo enviado em formato diferente de `application/json` |
| `422` | Unprocessable Entity | `X-Idempotency-Key` reutilizada com outro corpo |
| `429` | Too Many Requests | Limite de requisições excedido (veja o `Retry-After`) |
| `500` | Internal Server Error | Erro inesperado (o detalhe completo fica só no log do servidor) |

## Objetos da API

Nas tabelas abaixo, **Validação** descreve o que a API exige na entrada (`POST` e `PUT`).

### User

Dono de watchlists.

| Propriedade | Tipo | Validação / Detalhes |
|---|---|---|
| `id` | Inteiro | Gerado pelo banco (somente leitura) |
| `name` | String | Obrigatório, de 3 a 100 caracteres |
| `email` | String | Obrigatório, e-mail válido, até 150 caracteres. **Único** |

### Genre

Categoria de filmes. A relação com filmes é N:N.

| Propriedade | Tipo | Validação / Detalhes |
|---|---|---|
| `id` | Inteiro | Gerado pelo banco (somente leitura) |
| `nome` | String | Obrigatório, de 2 a 50 caracteres. **Único** |

### Movie

Catálogo principal.

| Propriedade | Tipo | Validação / Detalhes |
|---|---|---|
| `id` | Inteiro | Gerado pelo banco (somente leitura) |
| `titulo` | String | Obrigatório, de 2 a 150 caracteres |
| `anoLancamento` | Inteiro | Obrigatório, entre 1888 e 2100 |
| `duracao` | Inteiro | Obrigatório, em minutos, maior que 0 |
| `classificacaoIndicativa` | String | Obrigatório: `L`, `10`, `12`, `14`, `16` ou `18` |
| `genreIds` | Array de inteiros | **Só na entrada.** Opcional. Os gêneros precisam já existir |
| `generos` | Array de [Genre](#genre) | **Só na saída** (na versão 2, uma lista de nomes) |

### MovieDetails

Informações estendidas do filme. Relação **1:1** com Movie.

| Propriedade | Tipo | Validação / Detalhes |
|---|---|---|
| `id` | Inteiro | Gerado pelo banco (somente leitura) |
| `movieId` | Inteiro | Obrigatório. Filme dono dos detalhes (não pode ser trocado depois) |
| `sinopseLonga` | String | Opcional, até 4000 caracteres |
| `orcamento` | Decimal | Opcional, maior ou igual a 0 |
| `paisOrigem` | String | Opcional, até 60 caracteres |
| `idiomaOriginal` | String | Opcional, até 40 caracteres |
| `notasProducao` | String | Opcional, até 2000 caracteres |

O `PATCH` aceita os mesmos campos, **todos opcionais**, sem o `movieId`.

### Watchlist

Lista pessoal de filmes de um usuário.

| Propriedade | Tipo | Validação / Detalhes |
|---|---|---|
| `id` | Inteiro | Gerado pelo banco (somente leitura) |
| `nome` | String | Obrigatório, de 3 a 100 caracteres |
| `descricao` | String | Opcional, até 255 caracteres |
| `userId` | Inteiro | Obrigatório. Usuário dono da lista |

### WatchlistItem

Um filme dentro de uma watchlist.

| Propriedade | Tipo | Validação / Detalhes |
|---|---|---|
| `id` | Inteiro | Gerado pelo banco (somente leitura) |
| `watchlistId` | Inteiro | Obrigatório (não pode ser trocado depois) |
| `movieId` | Inteiro | Obrigatório (não pode ser trocado depois) |
| `movieTitulo` | String | **Só na saída.** Título do filme |
| `status` | Enum | Obrigatório. Veja a tabela abaixo |
| `nota` | Inteiro | Opcional, de 1 a 10. **Só aceita nota quando o status é `VISTO`** |

| Status | Significado | Aceita nota? |
|---|---|---|
| `QUERO_VER` | Está na lista para assistir depois | Não |
| `VENDO` | Em andamento | Não |
| `VISTO` | Já assistido | **Sim** (1 a 10) |
| `ABANDONADO` | Desistiu no meio do caminho | Não |

## Referência de endpoints

Todas as rotas abaixo ficam sob `/api`. Na versão 2, lembre-se do cabeçalho `X-API-Key`.

### Users

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/users` | Lista usuários (paginado) |
| `GET` | `/users/{id}` | Busca usuário por ID |
| `POST` | `/users` | Cria usuário |
| `PUT` | `/users/{id}` | Atualiza usuário (substitui o recurso inteiro) |
| `DELETE` | `/users/{id}` | Remove usuário |
| `GET` | `/users/{userId}/watchlists` | Watchlists do usuário (paginado) |

### Genres

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/genres` | Lista gêneros (paginado) |
| `GET` | `/genres/{id}` | Busca gênero por ID |
| `GET` | `/genres/search?nome=...` | Busca por parte do nome |
| `POST` | `/genres` | Cria gênero |
| `PUT` | `/genres/{id}` | Atualiza gênero |
| `DELETE` | `/genres/{id}` | Remove gênero |
| `GET` | `/genres/{genreId}/movies` | Filmes do gênero (paginado) |

### Movies

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/movies` | Lista filmes (paginado) |
| `GET` | `/movies/{id}` | Busca filme por ID (formato v1) |
| `GET` | `/movies/v2/{id}` | Busca filme por ID (formato v2) |
| `GET` | `/movies/search?titulo=...` | Busca por parte do título (paginado) |
| `POST` | `/movies` | Cria filme |
| `PUT` | `/movies/{id}` | Atualiza filme |
| `DELETE` | `/movies/{id}` | Remove filme |
| `GET` | `/movies/{movieId}/details` | Detalhes do filme |

### Movie details

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/movie-details` | Lista detalhes (paginado) |
| `GET` | `/movie-details/{id}` | Busca detalhes por ID |
| `GET` | `/movie-details/search?paisOrigem=...` | Busca por país de origem (paginado) |
| `POST` | `/movie-details` | Cria detalhes |
| `PUT` | `/movie-details/{id}` | Atualiza (substituição completa) |
| `PATCH` | `/movie-details/{id}` | Atualiza (parcial) |
| `DELETE` | `/movie-details/{id}` | Remove detalhes |

### Watchlists

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/watchlists` | Lista watchlists (paginado) |
| `GET` | `/watchlists/{id}` | Busca watchlist por ID |
| `POST` | `/watchlists` | Cria watchlist |
| `PUT` | `/watchlists/{id}` | Atualiza watchlist |
| `DELETE` | `/watchlists/{id}` | Remove watchlist (os itens dela são removidos junto) |
| `GET` | `/watchlists/{watchlistId}/items` | Itens da watchlist (paginado). Filtro opcional: `?status=VISTO` |

### Watchlist items

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/watchlist-items` | Lista itens (paginado) |
| `GET` | `/watchlist-items/{id}` | Busca item por ID |
| `POST` | `/watchlist-items` | Adiciona filme a uma watchlist |
| `PUT` | `/watchlist-items/{id}` | Atualiza item (status e nota) |
| `DELETE` | `/watchlist-items/{id}` | Remove item |

### Exemplo: do filme à watchlist

```bash
# 1. Criar um filme                                   → 201 Created + Location
curl -X POST https://cinema-service-sv1b.onrender.com/api/movies \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Oppenheimer",
    "anoLancamento": 2023,
    "duracao": 180,
    "classificacaoIndicativa": "16",
    "genreIds": [1, 3]
  }'

# 2. Adicionar à watchlist 1 (use o id devolvido acima no lugar de 4)
curl -X POST https://cinema-service-sv1b.onrender.com/api/watchlist-items \
  -H "Content-Type: application/json" \
  -d '{ "watchlistId": 1, "movieId": 4, "status": "QUERO_VER" }'

# 3. Marcar como visto, com nota
curl -X PUT https://cinema-service-sv1b.onrender.com/api/watchlist-items/1 \
  -H "Content-Type: application/json" \
  -d '{ "watchlistId": 1, "movieId": 4, "status": "VISTO", "nota": 9 }'

# 4. Consultar só os filmes já vistos
curl "https://cinema-service-sv1b.onrender.com/api/watchlists/1/items?status=VISTO"
```

## Regras de negócio

- O **nome de um gênero** e o **e-mail de um usuário** não podem se repetir (`409`).
- Cada **filme** tem no máximo um registro de detalhes (`409`). O `movieId` dos detalhes não pode ser trocado (`400`).
- O mesmo filme não pode aparecer **duas vezes** na mesma watchlist (`409`). A watchlist e o filme de um item não podem ser trocados (`400`).
- A **nota** só pode ser informada quando o status do item é `VISTO` (`400`).
- Um **gênero associado a filmes** não pode ser removido (`409`).
- Um **filme que está em alguma watchlist** não pode ser removido (`409`). Ao remover um filme, os detalhes dele vão junto.
- Um **usuário que ainda possui watchlists** não pode ser removido (`409`). Ao remover uma watchlist, os itens dela vão junto.
- O `PUT` **substitui o recurso inteiro**: envie todos os campos. Para alterar só alguns campos de um detalhe de filme, use `PATCH /api/movie-details/{id}`.

## Executando o projeto

### Pré-requisitos

- **JDK 17+** e Maven (ou o `mvnw` incluso)
- **MySQL 8**

### Banco de dados

O esquema e os dados de exemplo ficam em `src/main/resources/banco.sql`. Como `spring.sql.init.mode=never`, o script **não roda sozinho**: execute-o uma vez.

```bash
mysql -u root -p < src/main/resources/banco.sql
```

Ele cria o banco `cinema_db` e insere **3 usuários, 4 gêneros, 3 filmes** (Interestelar, Matrix e O Auto da Compadecida), com detalhes, 2 watchlists e 3 itens.

### Configuração

As principais propriedades ficam em `src/main/resources/application.properties`. Qualquer uma pode ser sobrescrita por **variável de ambiente**.

| Propriedade | Descrição |
|---|---|
| `spring.datasource.url` / `username` / `password` | Conexão com o MySQL |
| `server.port` | Porta (padrão: variável `PORT`, ou `8080`) |
| `api-key.valid-keys` | Chaves aceitas na versão 2, separadas por vírgula |
| `rate-limit.capacity`, `rate-limit.refill-tokens`, `rate-limit.refill-seconds` | [Limite de requisições](#limite-de-requisições) |
| `cors.allowed-origins` | Origens autorizadas no [CORS](#cors) |
| `springdoc.server.url` | URL pública exibida no Swagger |

> Por padrão a aplicação procura o MySQL no host `mysqldb` (nome do container na rede Docker). Para rodar com um MySQL local, sobrescreva a conexão.

### Executando com Maven

```bash
git clone https://github.com/TiagoAntunes-Dev/cinema-service.git
cd cinema-service

SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/cinema_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=sua_senha \
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080` e o Swagger em `http://localhost:8080/swagger-ui/index.html`.

### Executando com Docker

O `Dockerfile` usa **build em dois estágios** (compila com JDK 17 e executa só com o JRE, mais leve).

```bash
docker build -t cinelog-api .

# A aplicação encontra o MySQL pelo host "mysqldb": rode ambos na mesma rede
docker network create cinema-net
docker run -d --name mysqldb --network cinema-net \
  -e MYSQL_ROOT_PASSWORD=sua_senha -e MYSQL_DATABASE=cinema_db mysql:8

docker run -p 8080:8080 --network cinema-net \
  -e SPRING_DATASOURCE_PASSWORD=sua_senha cinelog-api
```

### Deploy

A versão de produção roda no **Render**, a partir do `Dockerfile`. A aplicação lê a porta da variável `PORT` e confia nos cabeçalhos de encaminhamento do proxy (`server.forward-headers-strategy=framework`).

## Arquitetura e estrutura

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.4.3 (Spring Web MVC) |
| Persistência | Spring Data JPA + Hibernate |
| Banco de dados | MySQL (H2 apenas nos testes) |
| Hipermídia | Spring HATEOAS |
| Validação | Bean Validation (Jakarta) |
| Limite de requisições | Bucket4j 8.10.1 (Token Bucket) |
| Documentação | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| Build e deploy | Maven, Docker, Render |

A API segue **arquitetura em camadas**:

```
Controller  →  recebe a requisição HTTP e delega
DTO         →  contratos de entrada e saída (Request / Response)
Service     →  regras de negócio
Repository  →  acesso ao banco (Spring Data JPA)
Entity      →  mapeamento objeto-relacional
Exception   →  exceções customizadas + @RestControllerAdvice (formato único de erro)
```

**Fluxo de uma requisição** em `/api/**`: `ApiKeyInterceptor` (chave de API) → `RateLimitInterceptor` (limite) → Controller → `IdempotencyService` (nos `POST`) → Service → Repository. A ordem importa: a chave é conferida **antes** de contar a requisição no limite.

```
cinema-service/
├── src/main/java/com/cinelog/api/
│   ├── Configuration/   # CORS, OpenAPI, interceptors (chave de API, limite), versionamento, filtro do tema do Swagger
│   ├── Controllers/     # Endpoints REST
│   ├── DTO/             # Request / Response
│   ├── Entity/          # Entidades JPA e o enum WatchStatus
│   ├── Exception/       # Exceções customizadas + GlobalExceptionHandler
│   ├── Repository/      # Interfaces Spring Data JPA
│   └── Service/         # Regras de negócio, ApiKeyService, RateLimitService, IdempotencyService
├── src/main/resources/
│   ├── application.properties
│   ├── banco.sql        # Esquema e dados de exemplo (MySQL)
│   └── static/custom.css  # Tema do Swagger UI (claro e escuro)
├── postman/             # Coleção e ambiente do Postman
├── Dockerfile
└── pom.xml
```

## Testes e coleção do Postman

![Coleção Postman rodando com 66 testes aprovados](postman/postman-tests.png)

A pasta `postman/` traz a coleção **🎬 Cinelog API**, com **57 requisições** e **66 testes automatizados**, organizadas por recurso (Users, Genres, Movies, MovieDetails, Watchlists e WatchlistItems), e o ambiente `Cinelog_Local` com a variável `baseUrl` (`http://localhost:8080`).

1. Importe os dois arquivos no Postman e selecione o ambiente **Cinelog_Local**.
2. Execute a coleção com o **Collection Runner**.

> A coleção roda contra a **versão 1** (sem chave de API). Para exercitar a versão 2, adicione os cabeçalhos `X-API-Version: 2` e `X-API-Key`.

## Contribuições

Este é um **projeto acadêmico**. Sugestões e melhorias são bem-vindas via *issues* ou *pull requests*.

## Licença

Distribuído sob a licença **MIT**.

## Autor

**Tiago Antunes**

- GitHub: [@TiagoAntunes-Dev](https://github.com/TiagoAntunes-Dev)
- E-mail: tiagoantunes1974@gmail.com

---

<p align="center">Feito com ☕ e Spring Boot</p>
