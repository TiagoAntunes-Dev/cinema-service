package com.cinelog.api.Configuration;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ApiVersionSupport: descobre qual versão da API a requisição está usando e se ela é uma versão "protegida".
 *
 * As funcionalidades da Parte 2 (chave de API, rate limit e idempotência) só valem a partir da versão 2
 * (WebConfig.FIRST_SECURED_VERSION). Na versão 1 a API continua simples e aberta.
 * Este é o único lugar que decide isso, então os interceptors e o IdempotencyService ficam iguais entre si.
 */
public final class ApiVersionSupport {

    private ApiVersionSupport() {
    }

    // Número principal da versão pedida: "2", "2.0" e "v2" valem 2 (o Spring também aceita esses formatos).
    // Sem o cabeçalho, vale a versão padrão.
    public static int majorVersion(HttpServletRequest request) {
        String header = request.getHeader(WebConfig.API_VERSION_HEADER);
        if (header == null || header.isBlank()) {
            header = WebConfig.DEFAULT_API_VERSION;
        }
        return parseMajor(header);
    }

    // true quando a requisição usa a versão 2 ou superior.
    public static boolean isSecuredVersion(HttpServletRequest request) {
        return majorVersion(request) >= WebConfig.FIRST_SECURED_VERSION;
    }

    static int parseMajor(String version) {
        int defaultMajor = Integer.parseInt(WebConfig.DEFAULT_API_VERSION);
        String text = version.trim();

        int start = 0;
        while (start < text.length() && !Character.isDigit(text.charAt(start))) {
            start++; // ignora letras antes do número (ex.: o "v" de "v2")
        }
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        if (start == end) {
            return defaultMajor;
        }
        try {
            return Integer.parseInt(text.substring(start, end));
        } catch (NumberFormatException e) {
            return defaultMajor;
        }
    }
}
