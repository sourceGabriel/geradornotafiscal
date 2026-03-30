package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import org.springframework.stereotype.Service;

@Service
/**
 * Simula integracao com contas a receber para conciliacao financeira.
 */
public class FinanceiroService {

    /**
     * Envia dados da nota fiscal para o fluxo financeiro externo.
     *
     * @param notaFiscal nota fiscal emitida
     */
    public void enviarNotaFiscalParaContasReceber(NotaFiscal notaFiscal) {
        try {
            // Simula o envio da nota fiscal para o contas a receber.
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
