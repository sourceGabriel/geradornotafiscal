package br.com.itau.geradornotafiscal.service.idempotency;

public enum IdempotencyState {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
