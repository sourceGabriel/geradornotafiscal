package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.service.exception.IntegracaoNotaFiscalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Component
public class NotaFiscalIntegracaoFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotaFiscalIntegracaoFacade.class);

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
        CompletableFuture<Void> estoqueFuture = executarIntegracao("estoque",
                () -> estoqueService.enviarNotaFiscalParaBaixaEstoque(notaFiscal));
        CompletableFuture<Void> registroFuture = executarIntegracao("registro",
                () -> registroService.registrarNotaFiscal(notaFiscal));
        CompletableFuture<Void> entregaFuture = executarIntegracao("entrega",
                () -> entregaService.agendarEntrega(notaFiscal));
        CompletableFuture<Void> financeiroFuture = executarIntegracao("financeiro",
                () -> financeiroService.enviarNotaFiscalParaContasReceber(notaFiscal));

        try {
            CompletableFuture.allOf(estoqueFuture, registroFuture, entregaFuture, financeiroFuture).join();
            LOGGER.info("nota_fiscal={} event=integracoes_concluidas", notaFiscal.getIdNotaFiscal());
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IntegracaoNotaFiscalException integracaoEx) {
                throw integracaoEx;
            }
            throw new IntegracaoNotaFiscalException("Falha ao executar integracoes da nota fiscal", cause);
        }
    }

    private CompletableFuture<Void> executarIntegracao(String integracao, Runnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
                LOGGER.info("integracao={} event=integracao_concluida", integracao);
            } catch (RuntimeException ex) {
                LOGGER.error("integracao={} event=integracao_falhou message={}", integracao, ex.getMessage());
                throw new IntegracaoNotaFiscalException(
                        "Falha ao executar integracoes da nota fiscal: " + integracao,
                        ex
                );
            }
        }, notaFiscalExecutorService);
    }
}

