package br.com.itau.geradornotafiscal.service.idempotency;

import br.com.itau.geradornotafiscal.model.NotaFiscal;

import java.util.function.Supplier;

/**
 * Abstracao para controle de idempotencia na geracao de nota fiscal.
 */
public interface NotaFiscalIdempotencyStore {

    /**
     * Executa operacao deduplicando por chave.
     *
     * @param key chave idempotente derivada do payload
     * @param operation operacao de geracao da nota
     * @return nota fiscal reaproveitada ou recem-gerada
     */
    NotaFiscal execute(String key, Supplier<NotaFiscal> operation);
}

