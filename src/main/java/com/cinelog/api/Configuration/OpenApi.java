package com.cinelog.api.Configuration; // Alinhado com o padrão de pacotes do seu projeto

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

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version:1.0.0}") String appVersion) {
        return new OpenAPI()
                .info(new Info()
                        .title("Cinelog API — Catalogação de Filmes e Watchlists")
                        .version(appVersion)
                        .description("""
                                **API REST para catalogação de filmes e gerenciamento de watchlists pessoais.**

                                A Cinelog API permite cadastrar filmes, associá-los a gêneros e armazenar
                                informações detalhadas (sinopse, orçamento, país de origem). Cada usuário pode
                                criar listas pessoais de filmes (watchlists) e acompanhar o status de cada item
                                (**QUERO_VER / VENDO / VISTO / ABANDONADO**), com nota opcional de 1 a 10.

                                ---

                                ### 📦 Recursos disponíveis
                                - **Usuários** — donos das watchlists
                                - **Gêneros** — categorias de filmes
                                - **Filmes** — catálogo principal
                                - **Detalhes de Filmes** — informações estendidas (1:1 com filme)
                                - **Watchlists** — listas pessoais de cada usuário
                                - **Itens de Watchlist** — filmes dentro de uma watchlist (com status e nota)

                                ### 📄 Paginação
                                Endpoints que retornam listas são paginados:
                                `?page=0&size=20&sort=campo,asc`

                                ### 🔗 HATEOAS
                                Toda resposta de sucesso inclui um bloco `_links` com URLs para navegação
                                entre os recursos (ex.: `self`, `update`, `delete`, `movie`, `watchlist`).

                                ### 🚨 Tratamento de erros
                                Todos os erros retornam o mesmo formato padronizado:
                                `timestamp`, `status`, `errorMessage`, `message`, `path`.

                                ### 🌐 Base URL
                                `http://localhost:8080/api/v1`
                                """)
                        .termsOfService("https://swagger.io/terms/")
                        .license(new License()
                                .name("MIT")
                                .url("https://mit-license.org/"))
                        .contact(new Contact()
                                .name("Tiago Antunes")
                                .url("https://github.com/tiagoantunes")
                                .email("tiagoantunes1974@gmail.com")))
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
