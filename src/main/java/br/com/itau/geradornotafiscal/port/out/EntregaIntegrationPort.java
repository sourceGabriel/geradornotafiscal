package br.com.itau.geradornotafiscal.port.out;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class EntregaIntegrationPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntregaIntegrationPort.class);
    private final ObjectMapper objectMapper;

    public EntregaIntegrationPort(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void criarAgendamentoEntrega(NotaFiscal notaFiscal) {
        Instant inicio = Instant.now();
        long inicioNanos = System.nanoTime();
        try {
            // Mantém latência simulada para integração externa.
            int quantidadeItens = notaFiscal.getItens() == null ? 0 : notaFiscal.getItens().size();
            LOGGER.info("event= entrega_port_chamada_inicio started_at= {} request= {} quantidade_itens= {}",
                    inicio,
                    toJson(notaFiscal),
                    quantidadeItens);

            if (quantidadeItens > 5) {
                LOGGER.warn("event= entrega_port_resposta_inesperada detalhe= lote_grande_simulado quantidade_itens= {} delay_ms= 5000",
                        quantidadeItens);
                Thread.sleep(5000);
            }

            Thread.sleep(200);
            LOGGER.info("event= entrega_port_chamada_fim status= SUCCESS finished_at= {} duration_ms= {} response= {}",
                    Instant.now(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos),
                    "SUCCESS provider_ack_agendamento_criado");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("event= entrega_port_chamada_fim status= ERROR finished_at= {} duration_ms= {} response= {} error_message= {}",
                    Instant.now(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos),
                    "ERROR interrupted_exception",
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String toJson(Object obj){
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            LOGGER.error("Falha ao converter objeto para JSON: {}", ex.getMessage());
            return "error_serializing_response";
        }
    }
}
