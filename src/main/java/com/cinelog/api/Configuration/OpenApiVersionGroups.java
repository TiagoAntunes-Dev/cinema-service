package com.cinelog.api.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * OpenApiVersionGroups: cria uma "tela" do Swagger para cada versão da API.
 */
@Configuration
public class OpenApiVersionGroups {

    // Nome interno do esquema de segurança (aparece no botão Authorize).
    private static final String API_KEY_SCHEME = "ApiKeyAuth";

    @Bean
    public GroupedOpenApi apiVersion1() {
        return versionGroup(1);
    }

    @Bean
    public GroupedOpenApi apiVersion2() {
        return versionGroup(2);
    }

    private GroupedOpenApi versionGroup(int version) {
        String description = (version >= WebConfig.FIRST_SECURED_VERSION)
                ? "completa: chave de API, rate limit e idempotência"
                : "simples";

        return GroupedOpenApi.builder()
                .group("v" + version)                                      // vira /v3/api-docs/v1, /v3/api-docs/v2...
                .displayName("Versão " + version + " (" + description + ")") // texto do seletor
                .pathsToMatch("/api/**")
                .addOpenApiMethodFilter(method -> servesVersion(method, version))
                .addOpenApiCustomizer(openApi -> customizeDocument(openApi, version))
                .build();
    }

    // Removido o uso de mapping.version() (método não nativo do Spring Framework)
    private static boolean servesVersion(Method method, int groupVersion) {
        return true; // Exibe todos os endpoints /api/** em ambas as telas
    }

    static void customizeDocument(OpenAPI openApi, int version) {
        boolean secured = version >= WebConfig.FIRST_SECURED_VERSION;

        if (openApi.getPaths() != null) {
            for (PathItem pathItem : openApi.getPaths().values()) {
                for (Operation operation : pathItem.readOperations()) {
                    customizeOperation(operation, version, secured);
                }
            }
        }

        customizeMovieById(openApi, secured);

        if (secured) {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name(WebConfig.API_KEY_HEADER)
                    .description("Chave de API, obrigatória na versão 2. Clique em Authorize e informe uma das chaves "
                            + "configuradas em api-key.valid-keys (application.properties)."));
        }
    }

    // GET /api/movies/{id} devolve MovieResponse (v1) ou MovieResponseV2 (v2): cada tela mostra só o formato da sua versão.
    private static void customizeMovieById(OpenAPI openApi, boolean secured) {
        if (openApi.getPaths() == null) {
            return;
        }
        PathItem item = openApi.getPaths().get("/api/movies/{id}");
        if (item == null || item.getGet() == null || item.getGet().getResponses() == null) {
            return;
        }
        ApiResponse ok = item.getGet().getResponses().get("200");
        if (ok != null && ok.getContent() != null) {
            String schemaName = secured ? "MovieResponseV2" : "MovieResponse";
            for (io.swagger.v3.oas.models.media.MediaType mediaType : ok.getContent().values()) {
                mediaType.setSchema(new Schema<>().$ref("#/components/schemas/" + schemaName));
            }
        }
        // Na tela da versão 1, o formato da versão 2 não é usado: tira da seção Schemas.
        if (!secured && openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            openApi.getComponents().getSchemas().remove("MovieResponseV2");
        }
    }

    private static void customizeOperation(Operation operation, int version, boolean secured) {
        String number = String.valueOf(version);

        // 1) Cabeçalho X-API-Version preenchido com a versão da tela atual
        removeHeader(operation, WebConfig.API_VERSION_HEADER);
        operation.addParametersItem(new HeaderParameter()
                .name(WebConfig.API_VERSION_HEADER)
                .description("Versão da API desta tela")
                .required(false)
                .schema(new StringSchema()._default(number).addEnumItem(number)));

        if (!secured) {
            // 2) Versão 1: remove idempotência e resposta 422
            removeHeader(operation, WebConfig.IDEMPOTENCY_KEY_HEADER);
            if (operation.getResponses() != null) {
                operation.getResponses().remove("422");
            }
            return;
        }

        // 3) Versão 2: cadeado (chave de API) e respostas de erro 401 e 429
        operation.addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
        addErrorResponse(operation, "401", "Chave de API ausente ou inválida (cabeçalho X-API-Key)");
        addErrorResponse(operation, "429", "Limite de requisições excedido. Veja o cabeçalho Retry-After");
    }

    private static void removeHeader(Operation operation, String headerName) {
        if (operation.getParameters() != null) {
            operation.getParameters().removeIf(parameter -> headerName.equalsIgnoreCase(parameter.getName()));
        }
    }

    private static void addErrorResponse(Operation operation, String code, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null || responses.containsKey(code)) {
            return;
        }
        Schema<?> apiError = new Schema<>().$ref("#/components/schemas/APIError");
        responses.addApiResponse(code, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new io.swagger.v3.oas.models.media.MediaType().schema(apiError))));
    }
}