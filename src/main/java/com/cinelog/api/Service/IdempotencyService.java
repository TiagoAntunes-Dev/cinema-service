package com.cinelog.api.Service;

import com.cinelog.api.Configuration.ApiVersionSupport;
import com.cinelog.api.Exception.BusinessException;
import com.cinelog.api.Exception.UnprocessableRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * IdempotencyService: faz com que repetir a MESMA requisição POST não crie registros duplicados.
 *
 * O problema: o cliente envia um POST, a rede falha (ou ele clica duas vezes) e ele reenvia.
 * Sem proteção, o registro seria criado duas vezes.
 *
 * A solução: o cliente manda o cabeçalho X-Idempotency-Key com um valor único (ex.: um UUID).
 * - 1ª vez que a chave aparece: executa a operação normalmente e GUARDA a resposta.
 * - Chave repetida com o MESMO corpo: não executa nada; devolve a resposta guardada
 *   (e acrescenta o cabeçalho "Idempotent-Replayed: true" para o cliente saber que é uma repetição).
 * - Chave repetida com corpo DIFERENTE: erro 422 (UnprocessableRequestException), pois a chave
 *   deve identificar sempre a mesma operação.
 * - Sem o cabeçalho: comportamento normal (a idempotência é opcional).
 * - Só vale na versão 2 da API (X-API-Version: 2). Na versão 1 o cabeçalho é ignorado e o POST sempre cria.
 *
 * Observações:
 * - GET, PUT e DELETE já são idempotentes por definição do HTTP; por isso só os POST usam este service.
 * - As respostas ficam em memória (como o banco H2 da aplicação, somem ao reiniciar) e expiram em 24 horas.
 */
@Service
public class IdempotencyService {

    // Quanto tempo uma resposta guardada continua valendo.
    private static final Duration TTL = Duration.ofHours(24);

    // A limpeza das respostas expiradas roda, no máximo, uma vez por minuto.
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000;

    private static final int MAX_KEY_LENGTH = 255;

    // O que guardamos para cada chave: o corpo recebido (para comparar), a resposta devolvida e quando foi criada.
    // O corpo é o próprio DTO (record), que já tem equals() pronto: compara campo a campo.
    private record StoredResponse(Object request, ResponseEntity<?> response, Instant createdAt) {
    }

    // ConcurrentHashMap é seguro para várias requisições ao mesmo tempo (threads).
    private final Map<String, StoredResponse> store = new ConcurrentHashMap<>();

    private volatile long lastCleanup = System.currentTimeMillis();

    /**
     * Executa a operação respeitando a idempotência.
     *
     * @param key     valor do cabeçalho X-Idempotency-Key (pode ser nulo ou vazio)
     * @param request o corpo da requisição (DTO), usado para detectar a reutilização da chave com outro corpo
     * @param action  a operação de verdade (chamar o service e montar a resposta com os links HATEOAS)
     */
    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> execute(String key, Object request, Supplier<ResponseEntity<T>> action) {

        // Sem o cabeçalho (ou na versão 1 da API), a requisição segue o fluxo normal.
        if (key == null || key.isBlank() || !isEnabledForCurrentRequest()) {
            return action.get();
        }

        String idempotencyKey = key.trim();
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new BusinessException("O cabeçalho X-Idempotency-Key deve ter no máximo " + MAX_KEY_LENGTH + " caracteres.");
        }

        removeExpired();

        // computeIfAbsent é atômico: se duas requisições com a mesma chave chegarem juntas,
        // a segunda ESPERA a primeira terminar e então recebe a resposta guardada.
        // Se a operação lançar uma exceção (validação, 404, 409...), nada é guardado e o cliente pode tentar de novo.
        boolean[] firstTime = {false};
        StoredResponse stored = store.computeIfAbsent(idempotencyKey, k -> {
            firstTime[0] = true;
            return new StoredResponse(request, action.get(), Instant.now());
        });

        // Mesma chave, corpo diferente: não é uma repetição, é um uso indevido da chave.
        if (!stored.request().equals(request)) {
            throw new UnprocessableRequestException(
                    "A chave de idempotência informada já foi usada com um corpo de requisição diferente. "
                            + "Use uma chave nova para uma nova operação.");
        }

        ResponseEntity<T> response = (ResponseEntity<T>) stored.response();

        // Primeira execução: devolve a resposta como ela é.
        if (firstTime[0]) {
            return response;
        }

        // Repetição: devolve a mesma resposta (status, Location e corpo), marcada como "replayed".
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header("Idempotent-Replayed", "true")
                .body(response.getBody());
    }

    // A idempotência é um recurso da versão 2. Fora de uma requisição web (ex.: testes), funciona normalmente.
    private boolean isEnabledForCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return ApiVersionSupport.isSecuredVersion(attributes.getRequest());
        }
        return true;
    }

    /**
     * Sem esta limpeza, o mapa guardaria as respostas para sempre (vazamento de memória).
     */
    private void removeExpired() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        lastCleanup = now;
        store.values().removeIf(stored -> stored.createdAt().plus(TTL).isBefore(Instant.now()));
    }
}
