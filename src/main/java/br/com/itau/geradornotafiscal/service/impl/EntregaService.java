package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.port.out.EntregaIntegrationPort;
import org.springframework.stereotype.Service;

@Service
/**
 * Servico de dominio para orquestrar o agendamento de entrega da nota fiscal.
 */
public class EntregaService {

    private final EntregaIntegrationPort entregaIntegrationPort;

    public EntregaService(EntregaIntegrationPort entregaIntegrationPort) {
        this.entregaIntegrationPort = entregaIntegrationPort;
    }

    /**
     * Executa o fluxo de entrega mantendo latencia simulada do cenario.
     *
     * @param notaFiscal nota fiscal a ser enviada para agendamento
     */
    public void agendarEntrega(NotaFiscal notaFiscal) {
        try {
            // Simula o agendamento da entrega.
            Thread.sleep(150);
            entregaIntegrationPort.criarAgendamentoEntrega(notaFiscal);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
