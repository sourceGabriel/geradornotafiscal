package br.com.itau.geradornotafiscal.service.idempotency;

import br.com.itau.geradornotafiscal.model.NotaFiscal;

import java.util.concurrent.CompletableFuture;

/**
 * Estrutura auxiliar para representar posse e estado de uma chave idempotente.
 *
 * @param key chave idempotente
 * @param owner indica se a thread atual e dona da execucao
 * @param state estado atual da entrada
 * @param future resultado compartilhado entre concorrentes
 */
public record IdempotencyHandle(
        String key,
        boolean owner,
        IdempotencyState state,
        CompletableFuture<NotaFiscal> future
) {
}
