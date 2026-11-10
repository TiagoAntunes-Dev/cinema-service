package com.cinelog.api.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@Tag(name = "Raiz", description = "Informações iniciais da API")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "Boas-vindas", description = "Retorna uma mensagem de boas-vindas e links úteis para começar a usar a API.")
    public ResponseEntity<EntityModel<Map<String, String>>> welcome() {
        
        Map<String, String> response = Map.of(
            "mensagem", "🎬 Bem-vindo à Cinelog API!",
            "descricao", "API REST para catalogar filmes e montar watchlists pessoais.",
            "documentacao", "Acesse a documentação interativa no Swagger UI.",
            "url_swagger", "/swagger-ui/index.html"
        );

        EntityModel<Map<String, String>> resource = EntityModel.of(response,
            linkTo(methodOn(RootController.class).welcome()).withSelfRel(),
            linkTo(methodOn(MovieController.class).listar(null)).withRel("filmes"),
            linkTo(methodOn(GenreController.class).listar(null)).withRel("generos"),
            linkTo(methodOn(UserController.class).listar(null)).withRel("usuarios")
        );

        return ResponseEntity.ok(resource);
    }
}