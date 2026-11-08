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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

/**
 * OpenApiVersionGroups: cria uma "tela" do Swagger para cada versão da API.
 *
 * No Swagger UI aparece, no topo da página, o seletor "Select a definition" com uma opção por versão.
 * Cada opção mostra só o que vale para aquela versão:
 *
 * QUAIS ENDPOINTS APARECEM
 * - endpoints SEM "version" no mapeamento valem para todas as versões, então aparecem em todas as telas;
 * - endpoints com version = "1" (fixa) aparecem só na tela da versão 1;
 * - endpoints com version = "2+" (a partir da 2) aparecem nas telas da versão 2 em diante.
 *
 * O QUE MUDA NA DOCUMENTAÇÃO DE CADA TELA
 * - Versão 1 (simples): sem chave de API, sem cabeçalho de idempotência, sem respostas 401/422/429.
 * - Versão 2 (completa): botão "Authorize" para informar a chave de API (X-API-Key) uma única vez,
 *   cadeado nos endpoints e as respostas 401 (chave inválida) e 429 (limite excedido) documentadas.
 * - Nas duas, o cabeçalho X-API-Version já vem preenchido com a versão da tela.
 *
 * Para criar a versão 3: inclua "3" em WebConfig.SUPPORTED_API_VERSIONS e acrescente aqui um @Bean apiVersion3().
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

    // Lê o atributo "version" do mapeamento (@GetMapping, @PostMapping... são formas de @RequestMapping).
    private static boolean servesVersion(Method method, int groupVersion) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        String mappedVersion = (mapping == null) ? "" : mapping.version();
        return isVersionIncluded(mappedVersion, groupVersion);
    }

    /**
     * Regra de quais endpoints aparecem na tela de uma versão.
     *
     * @param mappedVersion o "version" do mapeamento: vazio, "1" (fixa) ou "2+" (da 2 em diante)
     * @param groupVersion  a versão da tela do Swagger
     */
    static boolean isVersionIncluded(String mappedVersion, int groupVersion) {
        if (mappedVersion == null || mappedVersion.isBlank()) {
            return true; // sem version: vale para todas as versões
        }
        String version = mappedVersion.trim();
        boolean fromVersionOn = version.endsWith("+"); // "2+" = versão 2 e as seguintes
        String number = fromVersionOn ? version.substring(0, version.length() - 1) : version;
        try {
            int mapped = Integer.parseInt(number);
            return fromVersionOn ? groupVersion >= mapped : groupVersion == mapped;
        } catch (NumberFormatException e) {
            return true; // formato que este filtro não entende (ex.: "1.1"): não esconde o endpoint
        }
    }

    // Ajusta o documento já pronto de uma versão. Este método trabalha numa CÓPIA do documento,
    // então o que é feito na tela da versão 2 não aparece na tela da versão 1.
    static void customizeDocument(OpenAPI openApi, int version) {
        boolean secured = version >= WebConfig.FIRST_SECURED_VERSION;

        if (openApi.getPaths() != null) {
            for (PathItem pathItem : openApi.getPaths().values()) {
                for (Operation operation : pathItem.readOperations()) {
                    customizeOperation(operation, version, secured);
                }
            }
        }

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

    private static void customizeOperation(Operation operation, int version, boolean secured) {
        String number = String.valueOf(version);

        // 1) Cabeçalho X-API-Version: nesta tela ele vem preenchido com a versão da tela,
        //    para o "Try it out" chamar sempre a versão que está sendo exibida.
        removeHeader(operation, WebConfig.API_VERSION_HEADER);
        operation.addParametersItem(new HeaderParameter()
                .name(WebConfig.API_VERSION_HEADER)
                .description("Versão da API desta tela")
                .required(false)
                .schema(new StringSchema()._default(number).addEnumItem(number)));

        if (!secured) {
            // 2) Versão 1: a idempotência não existe, então o cabeçalho e a resposta 422 saem da documentação.
            removeHeader(operation, WebConfig.IDEMPOTENCY_KEY_HEADER);
            if (operation.getResponses() != null) {
                operation.getResponses().remove("422");
            }
            return;
        }

        // 3) Versão 2: cadeado (chave de API) e as respostas de erro desta versão.
        operation.addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
        addErrorResponse(operation, "401", "Chave de API ausente ou inválida (cabeçalho X-API-Key)");
        addErrorResponse(operation, "429", "Limite de requisições excedido. Veja o cabeçalho Retry-After");
    }

    private static void removeHeader(Operation operation, String headerName) {
        if (operation.getParameters() != null) {
            operation.getParameters().removeIf(parameter -> headerName.equalsIgnoreCase(parameter.getName()));
        }
    }

    // Documenta uma resposta de erro no formato APIError (se a operação ainda não tiver essa resposta).
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

