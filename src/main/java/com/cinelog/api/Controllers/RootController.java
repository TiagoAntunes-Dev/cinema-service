package com.cinelog.api.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Raiz", description = "Informações iniciais da API")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "Boas-vindas", description = "Retorna uma mensagem de boas-vindas e o link para a documentação.")
    public ResponseEntity<Map<String, String>> welcome() {
        
        Map<String, String> response = Map.of(
            "mensagem", "🎬 Bem-vindo à Cinelog API!",
            "descricao", "API REST para catalogar filmes e montar watchlists pessoais.",
            "documentacao", "Acesse a documentação interativa no Swagger UI.",
            "url_swagger", "/swagger-ui/index.html"
        );

        return ResponseEntity.ok(response);
    }
}