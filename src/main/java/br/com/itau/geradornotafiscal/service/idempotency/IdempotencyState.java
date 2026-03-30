package br.com.itau.geradornotafiscal.service.idempotency;

/**
 * Estados de uma chave idempotente no ciclo de processamento.
 */
public enum IdempotencyState {
    /** Operacao ainda em andamento. */
    IN_PROGRESS,
    /** Operacao finalizada com sucesso e resultado reaproveitavel. */
    COMPLETED,
    /** Operacao finalizada com erro e com TTL curto para novo processamento. */
    FAILED
}
