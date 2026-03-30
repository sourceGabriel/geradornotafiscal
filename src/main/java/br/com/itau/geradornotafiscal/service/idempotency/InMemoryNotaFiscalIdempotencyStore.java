package br.com.itau.geradornotafiscal.service.idempotency;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
/**
 * Implementacao in-memory de idempotencia com estados IN_PROGRESS/COMPLETED/FAILED e TTL.
 */
public class InMemoryNotaFiscalIdempotencyStore implements NotaFiscalIdempotencyStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryNotaFiscalIdempotencyStore.class);
    private static final String MDC_IDEMPOTENCY_KEY = "idempotency_key";

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final long completedTtlMillis;
    private final long failedTtlMillis;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    @Autowired
    public InMemoryNotaFiscalIdempotencyStore(
            @Value("${idempotencia.nota-fiscal.ttl-completed-seconds:600}") long completedTtlSeconds,
            @Value("${idempotencia.nota-fiscal.ttl-failed-seconds:30}") long failedTtlSeconds,
            MeterRegistry meterRegistry
    ) {
        this(completedTtlSeconds, failedTtlSeconds, Clock.systemUTC(), meterRegistry);
    }

    public InMemoryNotaFiscalIdempotencyStore(long completedTtlSeconds, long failedTtlSeconds) {
        this(completedTtlSeconds, failedTtlSeconds, Clock.systemUTC(), new SimpleMeterRegistry());
    }

    InMemoryNotaFiscalIdempotencyStore(long completedTtlSeconds, long failedTtlSeconds, Clock clock) {
        this(completedTtlSeconds, failedTtlSeconds, clock, new SimpleMeterRegistry());
    }

    InMemoryNotaFiscalIdempotencyStore(long completedTtlSeconds, long failedTtlSeconds, Clock clock, MeterRegistry meterRegistry) {
        this.completedTtlMillis = completedTtlSeconds * 1000;
        this.failedTtlMillis = failedTtlSeconds * 1000;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    /**
     * Executa operacao com deduplicacao concorrente por chave idempotente.
     */
    public NotaFiscal execute(String key, Supplier<NotaFiscal> operation) {
        MDC.put(MDC_IDEMPOTENCY_KEY, key);
        long now = clock.millis();
        Entry candidate = Entry.inProgress();
        Entry entry = entries.compute(key, (ignored, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return candidate;
            }
            return existing;
        });

        boolean owner = entry == candidate;

        if (!owner) {
            meterRegistry.counter("nota_fiscal.idempotency.reuse").increment();
            LOGGER.info("idempotency_key= {} state= {} event= idempotency_reuse", key, entry.state);
            try {
                return joinExisting(entry.future);
            } finally {
                MDC.remove(MDC_IDEMPOTENCY_KEY);
            }
        }

        LOGGER.info("idempotency_key= {} event= idempotency_owner_acquired", key);
        try {
            NotaFiscal notaFiscal = operation.get();
            complete(key, candidate.future, notaFiscal);
            LOGGER.info("idempotency_key= {} nota_fiscal= {} event= idempotency_completed", key, notaFiscal.getIdNotaFiscal());
            return notaFiscal;
        } catch (RuntimeException ex) {
            fail(key, candidate.future, ex);
            LOGGER.warn("idempotency_key= {} event= idempotency_failed message= {}", key, ex.getMessage());
            throw ex;
        } finally {
            MDC.remove(MDC_IDEMPOTENCY_KEY);
        }
    }

    @Scheduled(fixedDelayString = "${idempotencia.nota-fiscal.cleanup-interval-millis:30000}")
    /**
     * Remove entradas expiradas para evitar crescimento indefinido da estrutura em memoria.
     */
    public void cleanupExpiredEntries() {
        long now = clock.millis();
        entries.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private void complete(String key, CompletableFuture<NotaFiscal> expectedFuture, NotaFiscal notaFiscal) {
        long expiration = clock.millis() + completedTtlMillis;
        entries.computeIfPresent(key, (ignored, entry) -> {
            if (entry.future != expectedFuture) {
                return entry;
            }
            entry.future.complete(notaFiscal);
            return entry.toCompleted(expiration);
        });
    }

    private void fail(String key, CompletableFuture<NotaFiscal> expectedFuture, RuntimeException exception) {
        long expiration = clock.millis() + failedTtlMillis;
        entries.computeIfPresent(key, (ignored, entry) -> {
            if (entry.future != expectedFuture) {
                return entry;
            }
            entry.future.completeExceptionally(exception);
            return entry.toFailed(expiration);
        });
    }

    private NotaFiscal joinExisting(CompletableFuture<NotaFiscal> future) {
        try {
            return future.join();
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeCause) {
                throw runtimeCause;
            }
            throw ex;
        }
    }

    private static final class Entry {
        private final CompletableFuture<NotaFiscal> future;
        private final IdempotencyState state;
        private final long expiresAtMillis;

        private Entry(CompletableFuture<NotaFiscal> future, IdempotencyState state, long expiresAtMillis) {
            this.future = future;
            this.state = state;
            this.expiresAtMillis = expiresAtMillis;
        }

        static Entry inProgress() {
            return new Entry(new CompletableFuture<>(), IdempotencyState.IN_PROGRESS, Long.MAX_VALUE);
        }

        Entry toCompleted(long expiresAtMillis) {
            return new Entry(future, IdempotencyState.COMPLETED, expiresAtMillis);
        }

        Entry toFailed(long expiresAtMillis) {
            return new Entry(future, IdempotencyState.FAILED, expiresAtMillis);
        }

        boolean isExpired(long now) {
            return state != IdempotencyState.IN_PROGRESS && now >= expiresAtMillis;
        }
    }
}



