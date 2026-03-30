package br.com.itau.geradornotafiscal.service.idempotency;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
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
public class InMemoryNotaFiscalIdempotencyStore implements NotaFiscalIdempotencyStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryNotaFiscalIdempotencyStore.class);

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final long completedTtlMillis;
    private final long failedTtlMillis;
    private final Clock clock;

    @Autowired
    public InMemoryNotaFiscalIdempotencyStore(
            @Value("${idempotencia.nota-fiscal.ttl-completed-seconds:600}") long completedTtlSeconds,
            @Value("${idempotencia.nota-fiscal.ttl-failed-seconds:30}") long failedTtlSeconds
    ) {
        this(completedTtlSeconds, failedTtlSeconds, Clock.systemUTC());
    }

    InMemoryNotaFiscalIdempotencyStore(long completedTtlSeconds, long failedTtlSeconds, Clock clock) {
        this.completedTtlMillis = completedTtlSeconds * 1000;
        this.failedTtlMillis = failedTtlSeconds * 1000;
        this.clock = clock;
    }

    @Override
    public NotaFiscal execute(String key, Supplier<NotaFiscal> operation) {
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
            LOGGER.info("idempotency_key={} state={} event=idempotency_reuse", key, entry.state);
            return joinExisting(entry.future);
        }

        LOGGER.info("idempotency_key={} event=idempotency_owner_acquired", key);
        try {
            NotaFiscal notaFiscal = operation.get();
            complete(key, candidate.future, notaFiscal);
            LOGGER.info("idempotency_key={} nota_fiscal={} event=idempotency_completed", key, notaFiscal.getIdNotaFiscal());
            return notaFiscal;
        } catch (RuntimeException ex) {
            fail(key, candidate.future, ex);
            LOGGER.warn("idempotency_key={} event=idempotency_failed message={}", key, ex.getMessage());
            throw ex;
        }
    }

    @Scheduled(fixedDelayString = "${idempotencia.nota-fiscal.cleanup-interval-millis:30000}")
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



