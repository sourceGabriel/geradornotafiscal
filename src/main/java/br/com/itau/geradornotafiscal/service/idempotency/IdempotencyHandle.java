package br.com.itau.geradornotafiscal.service.idempotency;

import br.com.itau.geradornotafiscal.model.NotaFiscal;

import java.util.concurrent.CompletableFuture;

public record IdempotencyHandle(
        String key,
        boolean owner,
        IdempotencyState state,
        CompletableFuture<NotaFiscal> future
) {
}
