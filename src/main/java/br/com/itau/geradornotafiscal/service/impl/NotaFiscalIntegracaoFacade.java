package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.service.exception.IntegracaoNotaFiscalException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Faixada para orquestracao das integracoes externas simuladas da nota fiscal.
 * As integracoes independentes sao executadas em paralelo no pool configurado.
 */
@Component
public class NotaFiscalIntegracaoFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotaFiscalIntegracaoFacade.class);

    private final EstoqueService estoqueService;
    private final RegistroService registroService;
    private final EntregaService entregaService;
    private final FinanceiroService financeiroService;
    private final ExecutorService notaFiscalExecutorService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final RetryTemplate integracaoRetryTemplate;
    private final Map<String, AtomicInteger> falhasConsecutivas = Map.of(
            "estoque", new AtomicInteger(0),
            "registro", new AtomicInteger(0),
            "entrega", new AtomicInteger(0),
            "financeiro", new AtomicInteger(0)
    );

    /**
     * Construtor de conveniencia para cenarios sem injecao completa de metricas/retry.
     */
    public NotaFiscalIntegracaoFacade(EstoqueService estoqueService,
                                      RegistroService registroService,
                                      EntregaService entregaService,
                                      FinanceiroService financeiroService,
                                      ExecutorService notaFiscalExecutorService, ObjectMapper objectMapper) {
        this(
                estoqueService,
                registroService,
                entregaService,
                financeiroService,
                notaFiscalExecutorService,
                new SimpleMeterRegistry(),
                objectMapper,
                defaultRetryTemplate()
        );
    }

    /**
     * Construtor principal com metricas e politica de retry injetaveis.
     */
    @Autowired
    public NotaFiscalIntegracaoFacade(EstoqueService estoqueService,
                                      RegistroService registroService,
                                      EntregaService entregaService,
                                      FinanceiroService financeiroService,
                                      ExecutorService notaFiscalExecutorService,
                                      MeterRegistry meterRegistry,
                                      ObjectMapper objectMapper,
                                      RetryTemplate integracaoRetryTemplate) {
        this.estoqueService = estoqueService;
        this.registroService = registroService;
        this.entregaService = entregaService;
        this.financeiroService = financeiroService;
        this.notaFiscalExecutorService = notaFiscalExecutorService;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.integracaoRetryTemplate = integracaoRetryTemplate;
        registrarGaugesFalhasConsecutivas();
    }

    /**
     * Executa todas as integracoes de nota fiscal e falha rapidamente caso qualquer uma falhe.
     *
     * @throws IntegracaoNotaFiscalException quando ao menos uma integracao externa nao conclui
     */
    public void executarIntegracoes(NotaFiscal notaFiscal) {
        Instant inicio = Instant.now();
        long inicioNanos = System.nanoTime();

        LOGGER.info("event= integracoes_inicio started_at= {} request= {}", inicio, toJson(notaFiscal));

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
            Instant fim = Instant.now();
            long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
            LOGGER.info("event= integracoes_fim status= SUCCESS finished_at= {} duration_ms= {} response= {}",
                    fim,
                    duracaoMs,
                    "SUCCESS todas_integracoes_concluidas");
        } catch (CompletionException ex) {
            Instant fim = Instant.now();
            long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
            Throwable cause = ex.getCause();
            LOGGER.error("event= integracoes_fim status= ERROR finished_at= {} duration_ms= {} response= {} error_message= {}",
                    fim,
                    duracaoMs,
                    "ERROR falha_em_uma_ou_mais_integracoes",
                    cause == null ? "erro_desconhecido" : cause.getMessage());
            if (cause instanceof IntegracaoNotaFiscalException integracaoEx) {
                throw integracaoEx;
            }
            throw new IntegracaoNotaFiscalException("Falha ao executar integracoes da nota fiscal", cause);
        }
    }

    /**
     * Envelopa cada integracao em tarefa assincrona com tratamento uniforme de erro e log.
     */
    private CompletableFuture<Void> executarIntegracao(String integracao, Runnable task) {
        Map<String, String> contextoMdc = MDC.getCopyOfContextMap();
        return CompletableFuture.runAsync(() -> {
            if (contextoMdc != null) {
                MDC.setContextMap(contextoMdc);
            }
            long inicioNanos = System.nanoTime();
            AtomicInteger ultimaTentativa = new AtomicInteger(1);
            try {
                LOGGER.info("event= integracao_inicio integracao= {} started_at= {}", integracao, Instant.now());
                integracaoRetryTemplate.execute(context -> {
                    int attempt = context.getRetryCount() + 1;
                    ultimaTentativa.set(attempt);
                    if (attempt > 1) {
                        meterRegistry.counter("nota_fiscal.integracao.retry.attempts", "integracao", integracao).increment();
                        LOGGER.warn("event= integracao_retry integracao= {} attempt= {}", integracao, attempt);
                    }

                    task.run();
                    return null;
                }, context -> {
                    meterRegistry.counter("nota_fiscal.integracao.retry.exhausted", "integracao", integracao).increment();
                    Throwable lastThrowable = context.getLastThrowable();
                    String msg = lastThrowable == null ? "erro_desconhecido" : lastThrowable.getMessage();
                    throw new IntegracaoNotaFiscalException("Falha ao executar integracoes da nota fiscal: " + integracao + " apos retries", lastThrowable == null ? new RuntimeException(msg) : lastThrowable);
                });

                if (ultimaTentativa.get() > 1) {
                    meterRegistry.counter("nota_fiscal.integracao.success_after_retry", "integracao", integracao).increment();
                }

                falhasConsecutivas.get(integracao).set(0);
                meterRegistry.counter("nota_fiscal.integracao.success", "integracao", integracao).increment();
                long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
                LOGGER.info("event= integracao_fim integracao= {} status= SUCCESS finished_at= {} duration_ms= {}",
                        integracao,
                        Instant.now(),
                        duracaoMs);
            } catch (RuntimeException ex) {
                falhasConsecutivas.get(integracao).incrementAndGet();
                meterRegistry.counter("nota_fiscal.integracao.failure", "integracao", integracao).increment();
                long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
                LOGGER.error("event= integracao_fim integracao= {} status= ERROR finished_at= {} duration_ms= {} error_message= {}",
                        integracao,
                        Instant.now(),
                        duracaoMs,
                        ex.getMessage());
                if (ex instanceof IntegracaoNotaFiscalException integracaoEx) {
                    throw integracaoEx;
                }
                throw new IntegracaoNotaFiscalException(
                        "Falha ao executar integracoes da nota fiscal: " + integracao,
                        ex
                );
            } finally {
                meterRegistry.timer("nota_fiscal.integracao.duration", "integracao", integracao)
                        .record(System.nanoTime() - inicioNanos, TimeUnit.NANOSECONDS);
                MDC.clear();
            }
        }, notaFiscalExecutorService);
    }

    /**
     * Publica gauges com quantidade de falhas consecutivas por integracao.
     */
    private void registrarGaugesFalhasConsecutivas() {
        falhasConsecutivas.forEach((integracao, contador) ->
                meterRegistry.gauge("nota_fiscal.integracao.consecutive_failures", java.util.List.of(
                                io.micrometer.core.instrument.Tag.of("integracao", integracao)
                        ), contador, AtomicInteger::get));
    }

    /**
     * Politica default de retry com maximo de tentativas e backoff exponencial com jitter.
     */
    private static RetryTemplate defaultRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
        backOffPolicy.setInitialInterval(200);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(1200);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * Serializa objetos para log estruturado, com fallback seguro em caso de erro.
     */
    private String toJson(Object obj){
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            LOGGER.error("Falha ao converter objeto para JSON: {}", ex.getMessage());
            return "error_serializing_response";
        }
    }
}

