package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class NotaFiscalIntegracaoFacade {

    private final EstoqueService estoqueService;
    private final RegistroService registroService;
    private final EntregaService entregaService;
    private final FinanceiroService financeiroService;
    private final ExecutorService notaFiscalExecutorService;

    public NotaFiscalIntegracaoFacade(EstoqueService estoqueService,
                                      RegistroService registroService,
                                      EntregaService entregaService,
                                      FinanceiroService financeiroService,
                                      ExecutorService notaFiscalExecutorService) {
        this.estoqueService = estoqueService;
        this.registroService = registroService;
        this.entregaService = entregaService;
        this.financeiroService = financeiroService;
        this.notaFiscalExecutorService = notaFiscalExecutorService;
    }

    public void executarIntegracoes(NotaFiscal notaFiscal) {
        CompletableFuture<Void> estoqueFuture = CompletableFuture.runAsync(
                () -> estoqueService.enviarNotaFiscalParaBaixaEstoque(notaFiscal), notaFiscalExecutorService);
        CompletableFuture<Void> registroFuture = CompletableFuture.runAsync(
                () -> registroService.registrarNotaFiscal(notaFiscal), notaFiscalExecutorService);
        CompletableFuture<Void> entregaFuture = CompletableFuture.runAsync(
                () -> entregaService.agendarEntrega(notaFiscal), notaFiscalExecutorService);
        CompletableFuture<Void> financeiroFuture = CompletableFuture.runAsync(
                () -> financeiroService.enviarNotaFiscalParaContasReceber(notaFiscal), notaFiscalExecutorService);

        CompletableFuture.allOf(estoqueFuture, registroFuture, entregaFuture, financeiroFuture).join();
    }
}

