package com.cinelog.api.Configuration; // Alinhado com o padrão de pacotes do seu projeto

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenApi: configuração global da documentação (Swagger UI).
 * O texto em "description" aceita Markdown e aparece no topo da página do Swagger.
 * A ordem da lista de tags define a ordem dos grupos de endpoints na tela.
 *
 * Atenção: os nomes das tags abaixo precisam ser idênticos aos usados em
 * {@code @Tag(name = "...")} nos controllers. Por isso os emojis ficam só na descrição.
 */
@Configuration
public class OpenApi {

    // ⭐ Ordem desejada das tags no Swagger UI.
    // Usada pelo bean `tagOrderCustomizer()` para forçar a ordem —
    // porque por padrão o Swagger UI ordena alfabeticamente.
    private static final List<String> ORDEM_DAS_TAGS = List.of(
            "Usuários",
            "Gêneros",
            "Filmes",
            "Detalhes dos Filmes",
            "Watchlists",
            "Itens da Watchlist"
    );

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${springdoc.version:1.0.0}") String appVersion,
            @Value("${springdoc.server.url:http://localhost:8080}") String serverUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("🎬 Cinelog API — Catalogação de Filmes e Watchlists")
                        .version(appVersion)
                        .description("""
                                # 🍿 Cinelog API

A **Cinelog API** é uma API REST desenvolvida para catalogar filmes e gerenciar watchlists pessoais.

Com ela, é possível cadastrar filmes, associá-los a gêneros, armazenar informações complementares — como sinopse, orçamento e país de origem — e acompanhar os filmes que cada usuário deseja assistir, está assistindo ou já assistiu.

Também é possível atribuir notas de 1 a 10 aos filmes concluídos.

> 💡 **Dica:** utilize o botão **Try it out** em cada endpoint para testar as requisições diretamente pelo Swagger UI.

---

## 🌐 1. Informações gerais

| Informação | Descrição |
|---|---|
| **URL base (produção)** | `https://cinema-service-sv1b.onrender.com/api` |
| **URL base (desenvolvimento)** | `http://localhost:8080/api` |
| **Rotas** | `/users`, `/genres`, `/movies`, `/movie-details`, `/watchlists`, `/watchlist-items` |
| **Formato** | JSON com codificação UTF-8 |
| **Banco de dados** | MySQL |
| **Persistência** | Os dados permanecem armazenados entre reinicializações |

### Cabeçalhos HTTP

| Cabeçalho | Quando utilizar | Valor |
|---|---|---|
| `Content-Type` | Requisições POST, PUT e PATCH | `application/json` |
| `Accept` | Opcional | `application/json` ou `*/*` |
| `X-API-Version` | Versionamento da API | `1` ou `2` |
| `X-API-Key` | Obrigatório na versão 2 | Chave de API |
| `X-Idempotency-Key` | Opcional nos POST da versão 2 | Identificador único, como um UUID |

---

## 🔖 2. Versionamento da API

O versionamento é controlado pelo cabeçalho HTTP `X-API-Version`, e não pela URL.

Todas as versões utilizam as mesmas rotas, no formato `/api/...`.

### Como funciona

| Requisição | Comportamento |
|---|---|
| Sem o cabeçalho | Utiliza a versão padrão (`1`) |
| `X-API-Version: 1` | Executa a versão 1 |
| `X-API-Version: 2` | Executa a versão 2 |
| `X-API-Version: 99` | Retorna `400 Bad Request` |
| `X-API-Version: abc` | Retorna `400 Bad Request` |

> 🔀 **Importante:** cada versão possui sua própria documentação. Utilize o seletor **Select a definition**, localizado na parte superior do Swagger UI, para alternar entre elas.

### Comparação entre as versões

| Recurso | Versão 1 | Versão 2 |
|---|---|---|
| Chave de API | Não exige | Obrigatória (`401`) |
| Rate Limit | Sem limite | Por chave de API (`429`) |
| Idempotência | Ignorada | Disponível nos POST |
| `GET /movies/{id}` | `duracao` e gêneros como objetos | `duracaoMinutos` e gêneros como texto |

### Autenticação na versão 2

A versão 2 exige uma chave de API para acessar os endpoints.

Para configurá-la:

1. Acesse a documentação da versão 2.
2. Clique em **Authorize**.
3. Informe sua chave de API.
4. Confirme a autorização.

**Chave de demonstração:**

```text
cinelog-demo-key-123
```

---

## 🚦 3. Limite de requisições (Rate Limit)

O Rate Limit está disponível exclusivamente na versão 2.

A API utiliza o algoritmo **Token Bucket**, no qual cada cliente, identificado pela chave de API, possui um conjunto de tokens disponíveis para realizar requisições.

Cada requisição consome um token, enquanto o sistema reabastece o saldo gradualmente.

Quando o limite é atingido, a API retorna:

```http
429 Too Many Requests
```

### Configuração padrão

| Parâmetro | Valor |
|---|---|
| Capacidade máxima do balde | 100 tokens |
| Taxa de reposição | 100 tokens por minuto |

### Cabeçalhos de resposta

| Cabeçalho | Descrição |
|---|---|
| `X-RateLimit-Limit` | Capacidade máxima do balde |
| `X-RateLimit-Remaining` | Quantidade de tokens restantes |
| `Retry-After` | Tempo de espera, em segundos, antes de tentar novamente. Presente somente em respostas `429` |

---

## 🔁 4. Idempotência

A idempotência está disponível exclusivamente na versão 2.

Todos os endpoints POST aceitam o cabeçalho opcional `X-Idempotency-Key`, que evita a criação de registros duplicados quando uma requisição é enviada novamente.

Esse mecanismo é especialmente útil em situações como:

- Cliques duplicados em botões.
- Novas tentativas após um timeout.
- Reenvio de requisições devido a falhas de conexão.

### Comportamento

| Situação | Resultado |
|---|---|
| Chave inédita | Executa a operação e armazena a resposta por 24 horas |
| Mesma chave e mesmo corpo | Retorna a resposta original, sem criar outro registro |
| Mesma chave e corpo diferente | Retorna `422 Unprocessable Entity` |
| Sem o cabeçalho | Executa normalmente, sem controle de idempotência |

Quando uma resposta original é reutilizada, a API retorna:

- Status HTTP original (`201`).
- Cabeçalho `Location` original.
- Corpo da resposta original.
- Cabeçalho `Idempotent-Replayed: true`.

---

## 🌍 5. CORS (Cross-Origin Resource Sharing)

O CORS permite que aplicações front-end hospedadas em origens diferentes realizem requisições à API diretamente pelo navegador.

Para que os cabeçalhos CORS sejam retornados, a requisição precisa incluir o cabeçalho `Origin`.

### Configuração

| Configuração | Valor |
|---|---|
| **Origens permitidas** | `http://localhost:3000`, `http://localhost:5173`, `http://localhost:4200` e o próprio domínio do Render |
| **Métodos permitidos** | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| **Cabeçalhos aceitos** | `Content-Type`, `Accept`, `X-API-Key`, `X-Idempotency-Key`, `X-API-Version` |
| **Cabeçalhos expostos** | `Location`, `Retry-After`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Idempotent-Replayed` |
| **Credenciais (cookies)** | Desativadas |
| **Cache do preflight** | 1 hora |

> ⚠️ **Atenção:** uma origem não autorizada recebe `403 Forbidden`, com a mensagem `Invalid CORS request`.

**Observação:** o CORS é uma proteção aplicada pelos navegadores. Ferramentas como Postman, Insomnia e cURL não são afetadas por essa restrição.

---

## 🗂️ 6. Recursos e relacionamentos

A API possui seis recursos principais:

| Recurso | Descrição | Relacionamento |
|---|---|---|
| 👤 **Usuários** | Representam os proprietários das watchlists | 1:N com Watchlists |
| 🎭 **Gêneros** | Categorias utilizadas para classificar filmes | N:N com Filmes |
| 🎬 **Filmes** | Catálogo principal de filmes | 1:1 com Detalhes |
| 📋 **Detalhes dos Filmes** | Armazenam sinopse, orçamento, país e idioma | 1:1 com Filme |
| 📺 **Watchlists** | Listas pessoais de cada usuário | 1:N com Itens |
| 🎞️ **Itens da Watchlist** | Associam filmes às listas, com status e nota | N:1 com Filme |

### Visão geral do modelo

```text
👤 Usuário
    │
    └── 📺 Watchlist
            │
            └── 🎞️ Item
                    │
                    └── 🎬 Filme
                            │
                            ├── 📋 Detalhes
                            │
                            └── 🎭 Gêneros
```

### Status de um item da Watchlist

Cada item pode assumir um dos seguintes status:

| Status | Significado | Aceita nota? |
|---|---|---|
| `QUERO_VER` | 📌 Filme adicionado para assistir futuramente | Não |
| `VENDO` | ▶️ Filme em andamento | Não |
| `VISTO` | ✅ Filme já assistido | Sim (1 a 10) |
| `ABANDONADO` | 🚫 Filme que o usuário desistiu de assistir | Não |

---

## 📄 7. Paginação

Os endpoints de listagem paginados aceitam os seguintes parâmetros:

| Parâmetro | Descrição | Padrão |
|---|---|---|
| `page` | Número da página. A primeira página é `0` | `0` |
| `size` | Quantidade de itens por página | `20` |
| `sort` | Campo e direção da ordenação | Sem ordenação |

### Exemplo de requisição

```http
GET /api/movies?page=0&size=10&sort=titulo,asc
```

A resposta contém:

- Lista de itens encontrados.
- Número da página atual (`number`).
- Quantidade de itens por página (`size`).
- Total de registros (`totalElements`).
- Total de páginas (`totalPages`).
- Links para navegação entre as páginas.

---

## 🔗 8. HATEOAS

A API utiliza **HATEOAS (Hypermedia as the Engine of Application State)** para fornecer links relacionados aos recursos nas respostas HTTP.

As respostas de sucesso que possuem corpo incluem um objeto `_links`, contendo URLs que permitem ao cliente navegar pelas operações disponíveis.

Exemplos de relações:

- `self`: endereço do próprio recurso.
- `update`: endereço para atualização.
- `delete`: endereço para remoção.
- `movie`: referência ao filme relacionado.
- `watchlist`: referência à watchlist relacionada.

Dessa forma, o cliente pode descobrir as operações disponíveis por meio dos links retornados pela própria API, reduzindo a necessidade de conhecer previamente todas as rotas.

---

## 🚦 9. Códigos de status HTTP

A API utiliza os seguintes códigos de status para representar os resultados das requisições:

| Código | Status | Descrição |
|---|---|---|
| 🟢 `200` | OK | Consulta ou atualização realizada com sucesso |
| 🟢 `201` | Created | Recurso criado com sucesso. O cabeçalho `Location` informa sua URL |
| 🟢 `204` | No Content | Recurso removido com sucesso, sem conteúdo na resposta |
| 🟠 `400` | Bad Request | Dados inválidos, JSON malformado, parâmetros incorretos, versão inexistente ou regra de negócio violada |
| 🟠 `401` | Unauthorized | Chave de API ausente ou inválida na versão 2 |
| 🟠 `403` | Forbidden | Origem não autorizada pelo CORS |
| 🟠 `404` | Not Found | Recurso não encontrado |
| 🟠 `405` | Method Not Allowed | Método HTTP não permitido para a rota |
| 🟠 `409` | Conflict | Registro duplicado ou ainda em uso |
| 🟠 `415` | Unsupported Media Type | Corpo enviado sem o formato `application/json` |
| 🟠 `422` | Unprocessable Entity | Chave de idempotência reutilizada com um corpo diferente |
| 🟠 `429` | Too Many Requests | Limite de requisições excedido |
| 🔴 `500` | Internal Server Error | Erro inesperado durante o processamento da requisição |

### Formato padrão das respostas de erro

Todas as respostas de erro seguem o mesmo formato:

```json
{
  "timestamp": "2026-10-04T14:55:57",
  "status": 404,
  "errorMessage": "NOT_FOUND",
  "message": "Filme não encontrado com o ID: 999",
  "path": "/api/movies/999"
}
```

### Erros de validação

Nas respostas `400 Bad Request` relacionadas à validação, a API também disponibiliza a propriedade `errors`, contendo os detalhes dos campos inválidos.

**Exemplo:**

```json
{
  "timestamp": "2026-10-04T14:55:57",
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

---

## 📏 10. Regras de negócio

A Cinelog API aplica regras de negócio para garantir a consistência dos dados e dos relacionamentos.

| Recurso | Regra |
|---|---|
| **Gêneros** | O nome de um gênero não pode se repetir |
| **Detalhes dos filmes** | Cada filme pode possuir, no máximo, um registro de detalhes |
| **Watchlist** | O mesmo filme não pode aparecer duas vezes na mesma watchlist |
| **Notas** | A nota, de 1 a 10, só pode ser informada quando o status do item for `VISTO` |
| **Filmes** | Um filme que esteja associado a alguma watchlist não pode ser removido |
| **Detalhes** | Ao remover um filme, seus detalhes são removidos junto |
| **Usuários** | Um usuário que ainda possui watchlists não pode ser removido |
| **Watchlists** | Ao remover uma watchlist, seus itens são removidos junto |

### PUT e PATCH

É importante entender a diferença entre os dois métodos:

- **PUT:** substitui o recurso inteiro. Portanto, todos os campos devem ser enviados na requisição.
- **PATCH:** permite atualizar parcialmente um recurso, enviando somente os campos que precisam ser alterados.

Na Cinelog API, o endpoint `PATCH /movie-details/{id}` permite modificar apenas alguns campos dos detalhes de um filme.

---

## 🚀 11. Como começar

Para testar a API do zero, siga a sequência abaixo:

| Ordem | Operação | Endpoint | Descrição |
|---|---|---|---|
| 1 | POST | `/genres` | Criar um gênero |
| 2 | POST | `/movies` | Criar um filme e associá-lo aos gêneros |
| 3 | POST | `/movie-details` | Adicionar os detalhes do filme |
| 4 | POST | `/users` | Criar um usuário |
| 5 | POST | `/watchlists` | Criar uma watchlist para o usuário |
| 6 | POST | `/watchlist-items` | Adicionar o filme à watchlist |

### Exemplo: criando um filme

**Endpoint:**

```http
POST /api/movies
```

**Corpo da requisição:**

```json
{
  "titulo": "Oppenheimer",
  "anoLancamento": 2023,
  "duracao": 180,
  "classificacaoIndicativa": "16",
  "genreIds": [1, 3]
}
```

Nesse exemplo, um filme é cadastrado com título, ano de lançamento, duração, classificação indicativa e os identificadores dos gêneros associados.

---

**Cinelog API — Documentação REST**

Desenvolvida para catalogar filmes e gerenciar watchlists pessoais.
                                """)
                        .license(new License()
                                .name("MIT")
                                .url("https://mit-license.org/"))
                        .contact(new Contact()
                                .name("Tiago Antunes")
                                .url("https://github.com/TiagoAntunes-Dev")
                                .email("tiagoantunes1974@gmail.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("📂 Repositório do projeto no GitHub")
                        .url("https://github.com/TiagoAntunes-Dev/cinema-service"))
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description(serverUrl.contains("onrender.com") ? "🚀 Servidor de Produção (Render)" : "💻 Servidor Local"),
                        new Server()
                                .url("https://cinema-service-sv1b.onrender.com")
                                .description("🌐 Produção (Render)"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("💻 Desenvolvimento Local")
                ))
                .tags(List.of(
                        new Tag().name("Usuários")
                                .description("Gestão de usuários (donos de watchlists)"),
                        new Tag().name("Gêneros")
                                .description("Gestão dos gêneros de filmes"),
                        new Tag().name("Filmes")
                                .description("Endpoints responsáveis pela gestão do catálogo de filmes"),
                        new Tag().name("Detalhes dos Filmes")
                                .description("Informações estendidas dos filmes (sinopse, orçamento, país de origem)"),
                        new Tag().name("Watchlists")
                                .description("Listas pessoais de filmes de cada usuário"),
                        new Tag().name("Itens da Watchlist")
                                .description("Endpoints responsáveis pelos filmes dentro de cada watchlist (status e nota)")
                ));
    }

    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            if (openApi.getTags() != null) {
                List<Tag> tags = new ArrayList<>(openApi.getTags());
                tags.sort(Comparator.comparingInt(tag -> {
                    int indice = ORDEM_DAS_TAGS.indexOf(tag.getName());
                    return indice == -1 ? Integer.MAX_VALUE : indice;
                }));
                openApi.setTags(tags);
            }
        };
    }

    // Ordem desejada dos Schemas (nomes EXATOS como aparecem na seção Schemas do Swagger)
    private static final List<String> ORDEM_DOS_SCHEMAS = List.of(
            // 👤 Usuários
            "UserRequest",
            "EntityModelUserResponse",
            "PagedModelEntityModelUserResponse",

            // 🎭 Gêneros
            "GenreRequest",
            "GenreResponse",
            "EntityModelGenreResponse",
            "PagedModelEntityModelGenreResponse",

            // 🎬 Filmes
            "MovieRequest",
            "EntityModelMovieResponse",
            "PagedModelEntityModelMovieResponse",

            // 📋 Detalhes dos Filmes
            "MovieDetailsRequest",
            "MovieDetailsPatchRequest",
            "EntityModelMovieDetailsResponse",
            "PagedModelEntityModelMovieDetailsResponse",

            // 📺 Watchlists
            "WatchListRequest",
            "EntityModelWatchListResponse",
            "PagedModelEntityModelWatchListResponse",

            // 🎞️ Itens da Watchlist
            "WatchListItemRequest",
            "EntityModelWatchListItemResponse",
            "PagedModelEntityModelWatchListItemResponse",

            // 🧩 Compartilhados
            "APIError",
            "PageMetadata",
            "Link",
            "Links"
    );

    @Bean
    @SuppressWarnings("rawtypes")
    public OpenApiCustomizer schemaOrderCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null || components.getSchemas() == null) return;

            Map<String, Schema> original = components.getSchemas();
            Map<String, Schema> ordenado = new LinkedHashMap<>();

            // 1º: os schemas da lista, na ordem definida
            for (String nome : ORDEM_DOS_SCHEMAS) {
                if (original.containsKey(nome)) {
                    ordenado.put(nome, original.get(nome));
                }
            }

            // 2º: os que sobraram (DTOs novos), em ordem alfabética
            original.entrySet().stream()
                    .filter(e -> !ordenado.containsKey(e.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> ordenado.put(e.getKey(), e.getValue()));

            components.setSchemas(ordenado);
        };
    }
}