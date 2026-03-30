package br.com.itau.geradornotafiscal.web.controller;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedido")
/**
 * Adapter HTTP para recebimento de pedidos e emissao de nota fiscal.
 * Mantem o contrato de entrada/saida exposto para sistemas consumidores.
 */
public class GeradorNFController {

    private static final String MDC_ID_PEDIDO = "id_pedido";

    private static final Logger log = LoggerFactory.getLogger(GeradorNFController.class);

    private final GeradorNotaFiscalService notaFiscalService;
    private final MeterRegistry meterRegistry;

    /**
     * Injeta dependencias de caso de uso e telemetria HTTP.
     *
     * @param notaFiscalService servico de orquestracao da nota fiscal
     * @param meterRegistry registro de metricas de tempo de endpoint
     */
    public GeradorNFController(GeradorNotaFiscalService notaFiscalService, MeterRegistry meterRegistry) {
        this.notaFiscalService = notaFiscalService;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/gerarNotaFiscal")
    /**
     * Endpoint principal de processamento de pedido.
     *
     * @param pedido payload de entrada do pedido
     * @return nota fiscal gerada quando validacoes e integracoes concluem com sucesso
     */
    public ResponseEntity<NotaFiscal> gerarNotaFiscal(@Valid @RequestBody Pedido pedido) {
        long inicioNanos = System.nanoTime();
        if (pedido.getIdPedido() != null) {
            MDC.put(MDC_ID_PEDIDO, String.valueOf(pedido.getIdPedido()));
        }

        int quantidadeItens = pedido.getItens() == null ? 0 : pedido.getItens().size();
        log.info("Recebida requisicao de nota fiscal. idPedido= {}, itens= {}", pedido.getIdPedido(), quantidadeItens);

        try {
            NotaFiscal notaFiscal = notaFiscalService.gerarNotaFiscal(pedido);
            log.info("Nota fiscal gerada com sucesso. idPedido= {}, idNotaFiscal= {}", pedido.getIdPedido(), notaFiscal.getIdNotaFiscal());
            return new ResponseEntity<>(notaFiscal, HttpStatus.OK);
        } finally {
            meterRegistry.timer("nota_fiscal.endpoint.duration", "endpoint", "gerarNotaFiscal")
                    .record(System.nanoTime() - inicioNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            MDC.remove(MDC_ID_PEDIDO);
        }
    }
}
