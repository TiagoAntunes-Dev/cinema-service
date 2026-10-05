package com.cinelog.api.Configuration; // Alinhado com o padrão de pacotes do seu projeto

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenApi: configuração global da documentação (Swagger UI).
 * O texto em "description" aceita Markdown e aparece no topo da página do Swagger.
 * A ordem da lista de tags define a ordem dos grupos de endpoints na tela.
 */
@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version:1.0.0}") String appVersion) {
        return new OpenAPI()
                .info(new Info()
                        .title("Cinelog API — Catalogação de Filmes e Watchlists")
                        .version(appVersion)
                        .description("""
                                A **Cinelog API** é uma API REST para catalogar filmes e montar watchlists pessoais.
                                Ela permite cadastrar filmes, ligá-los a gêneros, guardar detalhes (sinopse, orçamento,
                                país de origem) e acompanhar o que cada usuário quer ver, está vendo ou já viu,
                                com nota de 1 a 10.

                                ---

                                ### Detalhes do endpoint
                                - URL base: `http://localhost:8080/api/v1` (ambiente de desenvolvimento).
                                - As rotas usam substantivos no plural: `/users`, `/genres`, `/movies`,
                                  `/movie-details`, `/watchlists` e `/watchlist-items`.
                                - Os dados trafegam em JSON com codificação UTF-8 (acentos são aceitos).
                                - Esta versão da API não exige autenticação.
                                - O banco é o H2 em memória: os dados voltam ao estado inicial sempre que a aplicação reinicia.

                                ### Cabeçalhos
                                | Cabeçalho | Quando usar | Valor |
                                |---|---|---|
                                | `Content-Type` | Em POST, PUT e PATCH | `application/json` |
                                | `Accept` | Opcional | `application/json` ou `*/*` |

                                ### Recursos e relacionamentos
                                | Recurso | Descrição | Relacionamento |
                                |---|---|---|
                                | **Usuários** | Donos das watchlists | 1:N com Watchlists |
                                | **Gêneros** | Categorias de filmes | N:N com Filmes |
                                | **Filmes** | Catálogo principal | 1:1 com Detalhes |
                                | **Detalhes dos Filmes** | Sinopse, orçamento, país e idioma | 1:1 com Filme |
                                | **Watchlists** | Listas pessoais de cada usuário | 1:N com Itens |
                                | **Itens da Watchlist** | Um filme dentro de uma lista, com status e nota | N:1 com Filme |

                                O status de um item pode ser `QUERO_VER`, `VENDO`, `VISTO` ou `ABANDONADO`.

                                ### Paginação
                                Os endpoints de listagem paginados aceitam os parâmetros abaixo:

                                | Parâmetro | Descrição | Padrão |
                                |---|---|---|
                                | `page` | Número da página (a primeira é a 0) | `0` |
                                | `size` | Quantidade de itens por página | `20` |
                                | `sort` | Campo e direção da ordenação | sem ordenação |

                                Exemplo: `GET /api/v1/movies?page=0&size=10&sort=titulo,asc`

                                A resposta traz a lista de itens, as informações da página (`page`: `size`,
                                `totalElements`, `totalPages` e `number`) e os links para navegar entre as páginas.

                                ### HATEOAS
                                As respostas de sucesso com corpo incluem um bloco `_links` com as URLs que o cliente
                                pode seguir a partir dali (por exemplo `self`, `update`, `delete`, `movie` e `watchlist`).
                                Assim, não é preciso conhecer todas as rotas de antemão.

                                ### Códigos de status
                                | Código | Significado | Quando acontece |
                                |---|---|---|
                                | `200` | OK | Consulta ou atualização bem-sucedida |
                                | `201` | Created | Recurso criado (o header `Location` traz a URL dele) |
                                | `204` | No Content | Recurso removido, sem corpo de resposta |
                                | `400` | Bad Request | Dados inválidos ou regra de negócio violada |
                                | `404` | Not Found | Recurso não encontrado |
                                | `409` | Conflict | Registro duplicado ou ainda em uso |
                                | `500` | Internal Server Error | Erro inesperado no servidor |

                                ### Formato dos erros
                                Todos os erros retornam o mesmo formato:
                                ```
                                {
                                  "timestamp": "2026-10-04T14:55:57",
                                  "status": 404,
                                  "errorMessage": "NOT_FOUND",
                                  "message": "Filme não encontrado com o ID: 999",
                                  "path": "/api/v1/movies/999"
                                }
                                ```

                                ### Regras de negócio importantes
                                - O nome de um gênero não pode se repetir.
                                - Cada filme tem no máximo um registro de detalhes.
                                - O mesmo filme não pode aparecer duas vezes na mesma watchlist.
                                - A nota (1 a 10) só pode ser informada quando o status do item é `VISTO`.
                                - Um filme que está em alguma watchlist não pode ser removido. Ao remover um filme,
                                  os detalhes dele são removidos junto.
                                - Um usuário que ainda possui watchlists não pode ser removido. Ao remover uma watchlist,
                                  os itens dela são removidos junto.
                                - O `PUT` substitui o recurso inteiro: envie todos os campos.

                                ### Como começar
                                Para testar do zero, siga esta ordem:
                                1. Crie um gênero: `POST /genres`.
                                2. Crie um filme com `genreIds`: `POST /movies`.
                                3. Adicione os detalhes do filme: `POST /movie-details`.
                                4. Use um dos usuários de exemplo (ids 1, 2 e 3) ou crie outro: `POST /users`.
                                5. Crie uma watchlist para o usuário: `POST /watchlists`.
                                6. Adicione o filme à lista: `POST /watchlist-items`.
                                """)
                        .license(new License()
                                .name("MIT")
                                .url("https://mit-license.org/"))
                        .contact(new Contact()
                                .name("Tiago Antunes")
                                .url("https://github.com/TiagoAntunes-Dev")
                                .email("tiagoantunes1974@gmail.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repositório do projeto no GitHub")
                        .url("https://github.com/TiagoAntunes-Dev/cinema-service"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de desenvolvimento local")
                ))
                .tags(List.of(
                        new Tag().name("Usuários").description("Gestão de usuários (donos de watchlists)"),
                        new Tag().name("Gêneros").description("Gestão dos gêneros de filmes"),
                        new Tag().name("Filmes").description("Catálogo de filmes"),
                        new Tag().name("Detalhes dos Filmes").description("Informações estendidas dos filmes (sinopse, orçamento, país de origem)"),
                        new Tag().name("Watchlists").description("Listas pessoais de filmes de cada usuário"),
                        new Tag().name("Itens da Watchlist").description("Filmes dentro de uma watchlist, com status e nota")
                ));
    }
}