package br.com.itau.geradornotafiscal.port.out;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import org.springframework.stereotype.Component;

@Component
public class EntregaIntegrationPort {

    public void criarAgendamentoEntrega(NotaFiscal notaFiscal) {
        try {
            // Mantém latência simulada para integração externa.
            int quantidadeItens = notaFiscal.getItens() == null ? 0 : notaFiscal.getItens().size();
            if (quantidadeItens > 5) {
                Thread.sleep(5000);
            }
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
