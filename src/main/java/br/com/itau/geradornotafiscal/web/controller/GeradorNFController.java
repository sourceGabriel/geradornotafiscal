package br.com.itau.geradornotafiscal.web.controller;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedido")
public class GeradorNFController {

    private final GeradorNotaFiscalService notaFiscalService;

    public GeradorNFController(GeradorNotaFiscalService notaFiscalService) {
        this.notaFiscalService = notaFiscalService;
    }

    @PostMapping("/gerarNotaFiscal")
    public ResponseEntity<String> gerarNotaFiscal(@Valid @RequestBody Pedido pedido) {
        NotaFiscal notaFiscal = notaFiscalService.gerarNotaFiscal(pedido);
        String mensagem = "Nota fiscal gerada com sucesso para o pedido: " + pedido.getIdPedido();

        return new ResponseEntity<>(mensagem, HttpStatus.OK);
    }
}
