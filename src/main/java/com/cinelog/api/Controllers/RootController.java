package com.cinelog.api.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Raiz", description = "Informações iniciais da API e status do sistema")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "Boas-vindas", description = "Retorna uma mensagem de boas-vindas e links úteis para monitoramento e documentação.")
    public ResponseEntity<EntityModel<Map<String, String>>> welcome() {

        Map<String, String> response = Map.of(
                "mensagem", "🎬 Bem-vindo à Cinelog API!",
                "descricao", "API REST para catalogar filmes e montar watchlists pessoais.",
                "documentacao", "Acesse a documentação interativa no Swagger UI."
        );

        // Aqui criamos os links apontando para os endpoints do Actuator
        EntityModel<Map<String, String>> resource = EntityModel.of(response,
                Link.of("/swagger-ui/index.html").withRel("swagger"),
                Link.of("/actuator/health").withRel("health"),
                Link.of("/actuator/info").withRel("info"),
                Link.of("/actuator/metrics").withRel("metrics"),
                Link.of("/actuator/metrics/jvm.memory.used").withRel("memoria_jvm")
        );

        return ResponseEntity.ok(resource);
    }
}