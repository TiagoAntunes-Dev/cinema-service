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
 * - Cada cliente tem o seu próprio "balde" com uma CAPACIDADE máxima de tokens.
 * - Cada requisição gasta 1 token. Se o balde está vazio, a requisição é recusada (HTTP 429).
 * - O balde é reabastecido aos poucos (ex.: 100 tokens por minuto = 1 token a cada 0,6 s).
 * - Como o balde começa cheio, o cliente pode fazer uma "rajada" de requisições (burst) e depois
 *   precisa respeitar o ritmo de reabastecimento.
 *
 * Quem é o "cliente" depende da versão da API (ver RateLimitInterceptor):
 * - Versão 2: a CHAVE DE API (cada chave tem o seu balde).
 * - Versão 1: o ENDEREÇO IP de quem chama (a v1 é aberta, não tem chave).
 * Cada um tem a sua própria configuração, para que a v1 possa ser mais restrita que a v2.
 *
 * Os valores podem ser mudados no application.properties, sem mexer no código:
 *   rate-limit.capacity / refill-tokens / refill-seconds        -> versão 2 (por chave de API)
 *   rate-limit.v1.capacity / refill-tokens / refill-seconds     -> versão 1 (por IP)
 *     capacity       -> tamanho do balde (máximo de requisições em rajada)
 *     refill-tokens  -> quantos tokens voltam a cada período
 *     refill-seconds -> duração do período, em segundos
 */
@Service
public class RateLimitService {

    // A limpeza dos baldes ociosos roda, no máximo, uma vez por minuto.
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000;

    // Configuração de um tipo de cliente (chave de API ou IP).
    private record Policy(long capacity, long refillTokens, Duration refillPeriod) {
    }

    // Um balde e a capacidade dele (cada tipo de cliente tem a sua; a limpeza precisa saber qual é).
    private record Entry(Bucket bucket, long capacity) {
    }

    private final Policy apiKeyPolicy;
    private final Policy ipPolicy;

    // Um balde por cliente. ConcurrentHashMap é seguro para várias requisições ao mesmo tempo (threads).
    // A chave do mapa começa com "key:" (chave de API) ou "ip:" (endereço IP), então os dois tipos nunca se misturam.
    private final Map<String, Entry> buckets = new ConcurrentHashMap<>();

    private volatile long lastCleanup = System.currentTimeMillis();

    // Os números depois dos ":" são os valores padrão, usados se a propriedade não existir.
    public RateLimitService(@Value("${rate-limit.capacity:100}") long capacity,
                            @Value("${rate-limit.refill-tokens:100}") long refillTokens,
                            @Value("${rate-limit.refill-seconds:60}") long refillSeconds,
                            @Value("${rate-limit.v1.capacity:100}") long ipCapacity,
                            @Value("${rate-limit.v1.refill-tokens:100}") long ipRefillTokens,
                            @Value("${rate-limit.v1.refill-seconds:60}") long ipRefillSeconds) {
        this.apiKeyPolicy = new Policy(capacity, refillTokens, Duration.ofSeconds(refillSeconds));
        this.ipPolicy = new Policy(ipCapacity, ipRefillTokens, Duration.ofSeconds(ipRefillSeconds));
    }

    /**
     * Tenta gastar 1 token do balde da chave de API (versão 2).
     * O ConsumptionProbe devolve: se deu certo (isConsumed), quantos tokens sobraram
     * (getRemainingTokens) e quanto tempo falta para voltar 1 token (getNanosToWaitForRefill).
     */
    public ConsumptionProbe tryConsumeByApiKey(String apiKey) {
        return tryConsume("key:" + apiKey, apiKeyPolicy);
    }

    // Tenta gastar 1 token do balde do endereço IP (versão 1).
    public ConsumptionProbe tryConsumeByIp(String ip) {
        return tryConsume("ip:" + ip, ipPolicy);
    }

    // Usados no cabeçalho X-RateLimit-Limit.
    public long getApiKeyCapacity() {
        return apiKeyPolicy.capacity();
    }

    public long getIpCapacity() {
        return ipPolicy.capacity();
    }

    private ConsumptionProbe tryConsume(String clientKey, Policy policy) {
        removeFullBuckets();
        Entry entry = buckets.computeIfAbsent(clientKey, key -> new Entry(newBucket(policy), policy.capacity()));
        return entry.bucket().tryConsumeAndReturnRemaining(1);
    }

    private Bucket newBucket(Policy policy) {
        // Refill.greedy: os tokens voltam aos poucos (comportamento clássico do Token Bucket).
        // Refill.intervally (usado antes) devolvia todos os tokens de uma vez só ao fim do período.
        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.capacity())
                .refillGreedy(policy.refillTokens(), policy.refillPeriod())
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Sem esta limpeza, o mapa guardaria um balde para cada cliente que já acessou a API, para sempre
     * (vazamento de memória). Um balde CHEIO é igual a um balde novo, então pode ser apagado
     * sem prejudicar ninguém: se o cliente voltar, recebe um balde novo, também cheio.
     */
    private void removeFullBuckets() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        lastCleanup = now;
        buckets.values().removeIf(entry -> entry.bucket().getAvailableTokens() >= entry.capacity());
    }
}

