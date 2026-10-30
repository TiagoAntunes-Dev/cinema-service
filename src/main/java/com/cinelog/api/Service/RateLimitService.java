package com.cinelog.api.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimitService: aplica o algoritmo TOKEN BUCKET (balde de fichas) para limitar as requisições.
 *
 * Como o Token Bucket funciona:
 * - Cada cliente (identificado pelo IP) tem o seu próprio "balde" com uma CAPACIDADE máxima de tokens.
 * - Cada requisição gasta 1 token. Se o balde está vazio, a requisição é recusada (HTTP 429).
 * - O balde é reabastecido aos poucos (ex.: 100 tokens por minuto = 1 token a cada 0,6 s).
 * - Como o balde começa cheio, o cliente pode fazer uma "rajada" de requisições (burst) e depois
 *   precisa respeitar o ritmo de reabastecimento.
 *
 * Os valores podem ser mudados no application.properties, sem mexer no código:
 *   rate-limit.capacity       -> tamanho do balde (máximo de requisições em rajada)
 *   rate-limit.refill-tokens  -> quantos tokens voltam a cada período
 *   rate-limit.refill-seconds -> duração do período, em segundos
 */
@Service
public class RateLimitService {

    // A limpeza dos baldes ociosos roda, no máximo, uma vez por minuto.
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000;

    private final long capacity;
    private final long refillTokens;
    private final Duration refillPeriod;

    // Um balde por cliente. ConcurrentHashMap é seguro para várias requisições ao mesmo tempo (threads).
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private volatile long lastCleanup = System.currentTimeMillis();

    // Os números depois dos ":" são os valores padrão, usados se a propriedade não existir.
    public RateLimitService(@Value("${rate-limit.capacity:100}") long capacity,
                            @Value("${rate-limit.refill-tokens:100}") long refillTokens,
                            @Value("${rate-limit.refill-seconds:60}") long refillSeconds) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = Duration.ofSeconds(refillSeconds);
    }

    /**
     * Tenta gastar 1 token do balde do cliente.
     * O ConsumptionProbe devolve: se deu certo (isConsumed), quantos tokens sobraram
     * (getRemainingTokens) e quanto tempo falta para voltar 1 token (getNanosToWaitForRefill).
     */
    public ConsumptionProbe tryConsume(String clientKey) {
        removeFullBuckets();
        Bucket bucket = buckets.computeIfAbsent(clientKey, key -> newBucket());
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    // Usado no cabeçalho X-RateLimit-Limit.
    public long getCapacity() {
        return capacity;
    }

    private Bucket newBucket() {
        // Refill.greedy: os tokens voltam aos poucos (comportamento clássico do Token Bucket).
        // Refill.intervally (usado antes) devolvia todos os tokens de uma vez só ao fim do período.
        Bandwidth limit = Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(refillTokens, refillPeriod)
            .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Sem esta limpeza, o mapa guardaria um balde para cada IP que já acessou a API, para sempre
     * (vazamento de memória). Um balde CHEIO é igual a um balde novo, então pode ser apagado
     * sem prejudicar ninguém: se o cliente voltar, recebe um balde novo, também cheio.
     */
    private void removeFullBuckets() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        lastCleanup = now;
        buckets.values().removeIf(bucket -> bucket.getAvailableTokens() >= capacity);
    }
}