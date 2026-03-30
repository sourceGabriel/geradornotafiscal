package br.com.itau.geradornotafiscal.service.idempotency;

import br.com.itau.geradornotafiscal.model.NotaFiscal;

import java.util.function.Supplier;

public interface NotaFiscalIdempotencyStore {

    NotaFiscal execute(String key, Supplier<NotaFiscal> operation);
}

