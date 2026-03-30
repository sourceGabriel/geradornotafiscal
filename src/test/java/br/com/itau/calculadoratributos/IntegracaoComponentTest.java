package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.model.ItemNotaFiscal;
import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.port.out.EntregaIntegrationPort;
import br.com.itau.geradornotafiscal.service.exception.IntegracaoNotaFiscalException;
import br.com.itau.geradornotafiscal.service.impl.EntregaService;
import br.com.itau.geradornotafiscal.service.impl.EstoqueService;
import br.com.itau.geradornotafiscal.service.impl.FinanceiroService;
import br.com.itau.geradornotafiscal.service.impl.NotaFiscalIntegracaoFacade;
import br.com.itau.geradornotafiscal.service.impl.RegistroService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;

class IntegracaoComponentTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanupInterrupt() {
        if (Thread.interrupted()) {
            // interrupt flag cleared for test isolation
        }
        executorService.shutdownNow();
    }

    @Test
    void shouldExecuteLowLevelIntegrationServices() {
        NotaFiscal notaFiscal = notaFiscalComUmItem();

        new EstoqueService().enviarNotaFiscalParaBaixaEstoque(notaFiscal);
        new RegistroService().registrarNotaFiscal(notaFiscal);
        new FinanceiroService().enviarNotaFiscalParaContasReceber(notaFiscal);
    }

    @Test
    void shouldWrapInterruptedExceptionInLowLevelServices() {
        NotaFiscal notaFiscal = notaFiscalComUmItem();

        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, () -> new EstoqueService().enviarNotaFiscalParaBaixaEstoque(notaFiscal));
        assertTrue(Thread.currentThread().isInterrupted());
        if (Thread.interrupted()) {
            // interrupt flag cleared for next assertion
        }

        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, () -> new RegistroService().registrarNotaFiscal(notaFiscal));
        assertTrue(Thread.currentThread().isInterrupted());
        if (Thread.interrupted()) {
            // interrupt flag cleared for next assertion
        }

        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, () -> new FinanceiroService().enviarNotaFiscalParaContasReceber(notaFiscal));
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void shouldWrapInterruptedExceptionInEntregaPort() {
        EntregaIntegrationPort port = new EntregaIntegrationPort(new ObjectMapper());

        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, () -> port.criarAgendamentoEntrega(notaFiscalComUmItem()));
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void shouldDelegateEntregaServiceToPort() {
        EntregaIntegrationPort port = mock(EntregaIntegrationPort.class);
        EntregaService entregaService = new EntregaService(port);

        NotaFiscal notaFiscal = notaFiscalComUmItem();
        entregaService.agendarEntrega(notaFiscal);

        verify(port, times(1)).criarAgendamentoEntrega(notaFiscal);
    }

    @Test
    void shouldExecuteFacadeIntegrationsInParallel() {
        var estoque = mock(EstoqueService.class);
        var registro = mock(RegistroService.class);
        var entrega = mock(EntregaService.class);
        var financeiro = mock(FinanceiroService.class);

        NotaFiscalIntegracaoFacade facade = new NotaFiscalIntegracaoFacade(
                estoque,
                registro,
                entrega,
                financeiro,
                executorService,
                objectMapper
        );

        NotaFiscal notaFiscal = notaFiscalComUmItem();
        facade.executarIntegracoes(notaFiscal);

        verify(estoque, times(1)).enviarNotaFiscalParaBaixaEstoque(notaFiscal);
        verify(registro, times(1)).registrarNotaFiscal(notaFiscal);
        verify(entrega, times(1)).agendarEntrega(notaFiscal);
        verify(financeiro, times(1)).enviarNotaFiscalParaContasReceber(notaFiscal);
    }

    @Test
    void shouldThrowIntegracaoExceptionWhenAnyFacadeIntegrationFails() {
        var estoque = mock(EstoqueService.class);
        var registro = mock(RegistroService.class);
        var entrega = mock(EntregaService.class);
        var financeiro = mock(FinanceiroService.class);

        doThrow(new RuntimeException("falha externa"))
                .when(entrega)
                .agendarEntrega(org.mockito.ArgumentMatchers.any());

        NotaFiscalIntegracaoFacade facade = new NotaFiscalIntegracaoFacade(
                estoque,
                registro,
                entrega,
                financeiro,
                executorService,
                objectMapper
        );

        IntegracaoNotaFiscalException ex = assertThrows(
                IntegracaoNotaFiscalException.class,
                () -> facade.executarIntegracoes(notaFiscalComUmItem())
        );

        assertTrue(ex.getMessage().contains("entrega"));
    }

    @Test
    void shouldRetryAndSucceedWhenIntegrationFailsOnce() {
        var estoque = mock(EstoqueService.class);
        var registro = mock(RegistroService.class);
        var entrega = mock(EntregaService.class);
        var financeiro = mock(FinanceiroService.class);

        AtomicInteger tentativasEntrega = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (tentativasEntrega.incrementAndGet() == 1) {
                throw new RuntimeException("falha transitoria");
            }
            return null;
        }).when(entrega).agendarEntrega(org.mockito.ArgumentMatchers.any());

        NotaFiscalIntegracaoFacade facade = new NotaFiscalIntegracaoFacade(
                estoque,
                registro,
                entrega,
                financeiro,
                executorService,
                objectMapper
        );

        assertDoesNotThrow(() -> facade.executarIntegracoes(notaFiscalComUmItem()));
        assertEquals(2, tentativasEntrega.get());
    }

    @Test
    void shouldExhaustRetriesAndFailWhenIntegrationKeepsFailing() {
        var estoque = mock(EstoqueService.class);
        var registro = mock(RegistroService.class);
        var entrega = mock(EntregaService.class);
        var financeiro = mock(FinanceiroService.class);

        AtomicInteger tentativasEntrega = new AtomicInteger(0);
        doAnswer(invocation -> {
            tentativasEntrega.incrementAndGet();
            throw new RuntimeException("falha persistente");
        }).when(entrega).agendarEntrega(org.mockito.ArgumentMatchers.any());

        NotaFiscalIntegracaoFacade facade = new NotaFiscalIntegracaoFacade(
                estoque,
                registro,
                entrega,
                financeiro,
                executorService,
                objectMapper
        );

        assertThrows(IntegracaoNotaFiscalException.class, () -> facade.executarIntegracoes(notaFiscalComUmItem()));
        assertEquals(3, tentativasEntrega.get());
    }

    private NotaFiscal notaFiscalComUmItem() {
        return NotaFiscal.builder()
                .idNotaFiscal("nf-test")
                .itens(List.of(ItemNotaFiscal.builder()
                        .idItem("1")
                        .descricao("item")
                        .valorUnitario(10.0)
                        .quantidade(1)
                        .valorTributoItem(1.0)
                        .build()))
                .build();
    }
}



