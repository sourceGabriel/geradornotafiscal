package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import org.springframework.stereotype.Service;

@Service
/**
 * Simula integracao de baixa de estoque apos emissao da nota fiscal.
 */
public class EstoqueService {

    /**
     * Envia a nota fiscal para processamento de estoque com latencia simulada.
     *
     * @param notaFiscal nota fiscal emitida
     */
    public void enviarNotaFiscalParaBaixaEstoque(NotaFiscal notaFiscal) {
        try {
            // Simula envio de nota fiscal para baixa de estoque.
            Thread.sleep(380);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
