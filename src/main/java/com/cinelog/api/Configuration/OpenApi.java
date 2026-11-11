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
                                ## 🍿 Bem-vindo à Cinelog API

                                A **Cinelog API** é uma API REST para **catalogar filmes** e **montar watchlists pessoais**.
                                Cadastre filmes, ligue-os a gêneros, guarde detalhes (sinopse, orçamento, país de origem)
                                e acompanhe o que cada usuário quer ver, está vendo ou já viu, com nota de 1 a 10.

                                > 💡 **Dica:** use o botão **Try it out** em cada endpoint para testar direto desta página.

                                ---

                                ## 🌐 Detalhes do endpoint

                                | | Informação |
                                |---|---|
                                | 🔗 **URL base** | `http://localhost:8080/api` (desenvolvimento) |
                                | 🧭 **Rotas** | Substantivos no plural: `/users`, `/genres`, `/movies`, `/movie-details`, `/watchlists`, `/watchlist-items` |
                                | 📦 **Formato** | JSON com codificação UTF-8 (acentos são aceitos) |
                                | 🔓 **Autenticação** | Não exigida nesta versão |
                                | 🗄️ **Banco de dados** | H2 em memória |

                                > ⚠️ **Atenção:** como o banco é em memória, os dados **voltam ao estado inicial**
                                > sempre que a aplicação reinicia.

                                ---

                                ## 📨 Cabeçalhos

                                | Cabeçalho | Quando usar | Valor |
                                |---|---|---|
                                | `Content-Type` | Em `POST`, `PUT` e `PATCH` | `application/json` |
                                | `Accept` | Opcional | `application/json` ou `*/*` |
                                | `X-API-Version` | Opcional | Versão da API. Padrão: `1`. Veja a seção de versionamento abaixo |
                                | `X-API-Key` | **Obrigatório na versão 2** | Chave de API. Use o botão **Authorize** na tela da versão 2 |
                                | `X-Idempotency-Key` | Opcional, nos `POST` da versão 2 | Texto único (ex.: UUID). Veja a seção de idempotência abaixo |

                                ---

                                ## 🔖 Versionamento

                                A versão da API é informada no **cabeçalho** `X-API-Version`, e **não na URL**: as rotas são sempre `/api/...`.

                                | Requisição | Resultado |
                                |---|---|
                                | Sem o cabeçalho | Usa a versão padrão (`1`) |
                                | `X-API-Version: 1` | Versão 1 |
                                | `X-API-Version: 2` | Versão 2 |
                                | `X-API-Version: 99` (inexistente) ou `abc` (formato inválido) | `400 Bad Request` |

                                > 🔀 **Cada versão tem a sua tela:** use o seletor **Select a definition** (no topo da página) para alternar entre a
                                > versão 1 e a versão 2.

                                | Recurso | Versão 1 (simples) | Versão 2 (completa) |
                                |---|---|---|
                                | Chave de API (`X-API-Key`) | não exige | **obrigatória** (`401`) |
                                | Rate limit | por **IP** (`429` + `Retry-After`) | por **chave de API** (`429` + `Retry-After`) |
                                | Idempotência (`X-Idempotency-Key`) | ignora | nos `POST` (`422` se o corpo mudar) |
                                | `GET /movies/{id}` | campo `duracao` | campo `duracaoMinutos` (gêneros continuam como objetos) |

                                > 🔑 Na tela da **versão 2**, clique em **Authorize** e informe a chave de API uma única vez.
                                > Chave de demonstração: `cinelog-demo-key-123`.

                                ---

                                ## 🚦 Limite de requisições (Rate Limit)

                                A API usa o algoritmo **Token Bucket**: cada cliente tem um "balde" de tokens. Cada requisição gasta **1 token**
                                e o balde é reabastecido aos poucos. Com o balde vazio, a API responde `429 Too Many Requests`.
                                
                                | Versão | Quem é o "cliente" | Valores padrão |
                                |---|---|---|
                                | 1 | O **endereço IP** de quem chama | rajada de **100 requisições** e **100 tokens por minuto** |
                                | 2 | A **chave de API** (`X-API-Key`) | rajada de **100 requisições** e **100 tokens por minuto** |

                                | Cabeçalho de resposta | Quando aparece | Significado |
                                |---|---|---|
                                | `X-RateLimit-Limit` | Sempre | Tamanho máximo do balde |
                                | `X-RateLimit-Remaining` | Sempre | Tokens que ainda restam (`0` quando bloqueado) |
                                | `Retry-After` | Só no `429` | Segundos de espera antes de tentar de novo |

                                ---

                                ## 🔁 Idempotência

                                **Somente na versão 2.** Todos os `POST` aceitam o cabeçalho opcional **`X-Idempotency-Key`**, que evita registros duplicados quando
                                o cliente repete a requisição (clique duplo, retry após timeout).

                                | Situação | Resultado |
                                |---|---|
                                | Chave nova | Executa normalmente e guarda a resposta por 24 horas |
                                | Mesma chave e **mesmo corpo** | Devolve a resposta original (`201`, `Location` e corpo) com o cabeçalho `Idempotent-Replayed: true`, sem criar outro registro |
                                | Mesma chave e **corpo diferente** | `422 Unprocessable Entity` |
                                | Sem o cabeçalho | Comportamento normal |

                                ---

                                ## 🗂️ Recursos e relacionamentos

                                | | Recurso | Descrição | Relacionamento |
                                |---|---|---|---|
                                | 👤 | **Usuários** | Donos das watchlists | 1:N com Watchlists |
                                | 🎭 | **Gêneros** | Categorias de filmes | N:N com Filmes |
                                | 🎬 | **Filmes** | Catálogo principal | 1:1 com Detalhes |
                                | 📋 | **Detalhes dos Filmes** | Sinopse, orçamento, país e idioma | 1:1 com Filme |
                                | 📺 | **Watchlists** | Listas pessoais de cada usuário | 1:N com Itens |
                                | 🎞️ | **Itens da Watchlist** | Um filme dentro de uma lista, com status e nota | N:1 com Filme |

                                **Visão geral do modelo:**

                                ```
                                👤 Usuário → 📺 Watchlist → 🎞️ Item → 🎬 Filme → 📋 Detalhes
                                                                       ↕
                                                                    🎭 Gênero
                                ```

                                ### 🏷️ Status de um item

                                | Status | Significado | Aceita nota? |
                                |---|---|---|
                                | `QUERO_VER` | 📌 Está na lista para assistir depois | ❌ Não |
                                | `VENDO` | ▶️ Em andamento | ❌ Não |
                                | `VISTO` | ✅ Já assistido | ✅ Sim (1 a 10) |
                                | `ABANDONADO` | 🚫 Desistiu no meio do caminho | ❌ Não |

                                ---

                                ## 📄 Paginação

                                Os endpoints de listagem paginados aceitam os parâmetros abaixo:

                                | Parâmetro | Descrição | Padrão |
                                |---|---|---|
                                | `page` | Número da página (a primeira é a `0`) | `0` |
                                | `size` | Quantidade de itens por página | `20` |
                                | `sort` | Campo e direção da ordenação | sem ordenação |

                                ** Exemplo: ** `GET /api/movies?page=0&size=10&sort=titulo,asc`

                                A resposta traz a lista de itens, as informações da página (`page`: `size`,
                                `totalElements`, `totalPages` e `number`) e os links para navegar entre as páginas.

                                ---

                                ## 🔗 HATEOAS

                                As respostas de sucesso com corpo incluem um bloco `_links` com as URLs que o cliente
                                pode seguir a partir dali (por exemplo `self`, `update`, `delete`, `movie` e `watchlist`).
                                Assim, **não é preciso conhecer todas as rotas de antemão**.

                                ---

                                ## 🚦 Códigos de status

                                | | Código | Significado | Quando acontece |
                                |---|---|---|---|
                                | 🟢 | `200` | OK | Consulta ou atualização bem-sucedida |
                                | 🟢 | `201` | Created | Recurso criado (o header `Location` traz a URL dele) |
                                | 🟢 | `204` | No Content | Recurso removido, sem corpo de resposta |
                                | 🟠 | `400` | Bad Request | Dados inválidos, JSON mal formado, parâmetro com tipo errado, `sort` com campo inexistente, versão da API inexistente ou regra de negócio violada |
                                | 🟠 | `404` | Not Found | Recurso não encontrado |
                                | 🟠 | `409` | Conflict | Registro duplicado ou ainda em uso |
                                | 🟠 | `401` | Unauthorized | Chave de API ausente ou inválida (versão 2) |
                                | 🟠 | `405` | Method Not Allowed | Método HTTP não aceito pela rota |
                                | 🟠 | `415` | Unsupported Media Type | Corpo enviado sem ser `application/json` |
                                | 🟠 | `422` | Unprocessable Entity | `X-Idempotency-Key` reutilizada com um corpo diferente |
                                | 🟠 | `429` | Too Many Requests | Limite de requisições excedido (veja o `Retry-After`) |
                                | 🔴 | `500` | Internal Server Error | Erro inesperado no servidor |
                                | 🔴 | `503` | Service Unavailable | Banco de dados indisponível no momento (veja o `Retry-After`) |

                                ### ❌ Formato dos erros

                                Todos os erros retornam o mesmo formato:

                                ```json
                                {
                                  "timestamp": "2026-10-04T14:55:57",
                                  "status": 404,
                                  "errorMessage": "NOT_FOUND",
                                  "message": "Filme não encontrado com o ID: 999",
                                  "path": "/api/movies/999"
                                }
                                ```

                                Nos erros de **validação** (`400`) a resposta também traz a lista `errors`, com um item por campo inválido:

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

                                ## 📏 Regras de negócio importantes

                                - 🎭 O nome de um **gênero** não pode se repetir.
                                - 📋 Cada **filme** tem no máximo um registro de detalhes.
                                - 🎞️ O mesmo filme não pode aparecer **duas vezes** na mesma watchlist.
                                - ⭐ A **nota** (1 a 10) só pode ser informada quando o status do item é `VISTO`.
                                - 🎬 Um filme que está em alguma watchlist **não pode ser removido**. Ao remover um filme,
                                  os detalhes dele são removidos junto.
                                - 👤 Um usuário que ainda possui watchlists **não pode ser removido**. Ao remover uma watchlist,
                                  os itens dela são removidos junto.

                                > ✏️ O `PUT` **substitui o recurso inteiro**: envie todos os campos.
                                > Para alterar só alguns campos de um detalhe de filme, use `PATCH /movie-details/{id}`.

                                ---

                                ## 🚀 Como começar

                                Para testar do zero, siga esta ordem:

                                1. 🎭 Crie um gênero: `POST /genres`
                                2. 🎬 Crie um filme com `genreIds`: `POST /movies`
                                3. 📋 Adicione os detalhes do filme: `POST /movie-details`
                                4. 👤 Use um dos usuários de exemplo (ids `1`, `2` e `3`) ou crie outro: `POST /users`
                                5. 📺 Crie uma watchlist para o usuário: `POST /watchlists`
                                6. 🎞️ Adicione o filme à lista: `POST /watchlist-items`

                                **Exemplo rápido** — criando um filme:

                                ```json
                                {
                                  "titulo": "Oppenheimer",
                                  "anoLancamento": 2023,
                                  "duracao": 180,
                                  "classificacaoIndicativa": "16",
                                  "genreIds": [1, 3]
                                }
                                ```
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