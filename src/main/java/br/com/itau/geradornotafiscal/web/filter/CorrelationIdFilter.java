package br.com.itau.geradornotafiscal.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Garante correlation-id por request para rastreabilidade ponta a ponta.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Header de entrada/saida para correlacao distribuida. */
    public static final String CORRELATION_HEADER = "Xitau-Correlation-Id";
    /** Chave MDC usada no layout estruturado de log. */
    public static final String CORRELATION_MDC_KEY = "correlation_id";
    private static final int MAX_CORRELATION_ID_LENGTH = 128;

    @Override
    /**
     * Propaga (ou gera) correlation-id por requisicao e limpa o MDC ao final.
     */
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId != null) {
            correlationId = correlationId.trim();
            if (correlationId.length() > MAX_CORRELATION_ID_LENGTH) {
                correlationId = correlationId.substring(0, MAX_CORRELATION_ID_LENGTH);
            }
        }

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_MDC_KEY);
        }
    }
}

