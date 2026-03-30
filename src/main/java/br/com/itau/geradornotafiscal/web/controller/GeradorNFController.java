package br.com.itau.geradornotafiscal.web.controller;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
public class GeradorNFController {

    private static final Logger log = LoggerFactory.getLogger(GeradorNFController.class);

    private final GeradorNotaFiscalService notaFiscalService;

    public GeradorNFController(GeradorNotaFiscalService notaFiscalService) {
        this.notaFiscalService = notaFiscalService;
    }

    @PostMapping("/gerarNotaFiscal")
    /**
     * Endpoint principal de processamento de pedido.
     *
     * @param pedido payload de entrada do pedido
     * @return nota fiscal gerada quando validacoes e integracoes concluem com sucesso
     */
    public ResponseEntity<NotaFiscal> gerarNotaFiscal(@Valid @RequestBody Pedido pedido) {
        int quantidadeItens = pedido.getItens() == null ? 0 : pedido.getItens().size();
        log.info("Recebida requisicao de nota fiscal. idPedido={}, itens={}", pedido.getIdPedido(), quantidadeItens);

        NotaFiscal notaFiscal = notaFiscalService.gerarNotaFiscal(pedido);
        log.info("Nota fiscal gerada com sucesso. idPedido={}, idNotaFiscal={}", pedido.getIdPedido(), notaFiscal.getIdNotaFiscal());
        return new ResponseEntity<>(notaFiscal, HttpStatus.OK);
    }
}
