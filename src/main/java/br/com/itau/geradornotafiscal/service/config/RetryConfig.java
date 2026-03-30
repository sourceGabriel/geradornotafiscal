package br.com.itau.geradornotafiscal.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configura politica de retry com backoff exponencial e jitter para integracoes externas.
 */
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate integracaoRetryTemplate(
            @Value("${integracao.retry.max-attempts:3}") int maxAttempts,
            @Value("${integracao.retry.initial-interval-millis:200}") long initialIntervalMillis,
            @Value("${integracao.retry.multiplier:2.0}") double multiplier,
            @Value("${integracao.retry.max-interval-millis:1200}") long maxIntervalMillis
    ) {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
        backOffPolicy.setInitialInterval(initialIntervalMillis);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxIntervalMillis);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}

