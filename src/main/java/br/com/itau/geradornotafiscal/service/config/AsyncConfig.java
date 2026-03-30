package br.com.itau.geradornotafiscal.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
/**
 * Configura recursos de execucao assincrona usados no fluxo de integracoes externas.
 */
public class AsyncConfig {

    @Bean(destroyMethod = "shutdown")
    /**
     * Cria um pool fixo para limitar concorrencia e evitar sobrecarga.
     *
     * @return executor compartilhado do processamento de integracoes
     */
    public ExecutorService notaFiscalExecutorService() {
        return Executors.newFixedThreadPool(4);
    }
}

