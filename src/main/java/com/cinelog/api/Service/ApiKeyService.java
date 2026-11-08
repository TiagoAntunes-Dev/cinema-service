package com.cinelog.api.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/**
 * ApiKeyService: guarda as chaves de API aceitas e confere se a chave recebida é uma delas.
 *
 * As chaves vêm do application.properties (api-key.valid-keys), separadas por vírgula.
 * Se nenhuma chave for configurada, NENHUMA requisição da versão 2 passa (a API falha "fechada", é o mais seguro).
 */
@Service
public class ApiKeyService {

    private final List<byte[]> validKeys;

    // O valor depois dos ":" é o padrão quando a propriedade não existe: lista vazia.
    public ApiKeyService(@Value("${api-key.valid-keys:}") String[] validKeys) {
        this.validKeys = Arrays.stream(validKeys)
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .map(key -> key.getBytes(StandardCharsets.UTF_8))
                .toList();
    }

    public boolean isValid(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        byte[] candidate = apiKey.trim().getBytes(StandardCharsets.UTF_8);

        // MessageDigest.isEqual compara em tempo constante: o tempo de resposta não revela
        // quantos caracteres da chave estavam certos (evita "timing attack").
        // Por isso também percorremos todas as chaves, sem parar na primeira que bater.
        boolean valid = false;
        for (byte[] key : validKeys) {
            if (MessageDigest.isEqual(key, candidate)) {
                valid = true;
            }
        }
        return valid;
    }
}
