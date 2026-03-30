package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import org.springframework.stereotype.Service;

@Service
/**
 * Simula integracao de registro oficial da nota fiscal em sistema externo.
 */
public class RegistroService {

    /**
     * Registra a nota fiscal em provedor externo com latencia simulada.
     *
     * @param notaFiscal nota fiscal emitida
     */
    public void registrarNotaFiscal(NotaFiscal notaFiscal) {
        try {
            // Simula o registro da nota fiscal.
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
