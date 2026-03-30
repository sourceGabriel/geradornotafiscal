package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.web.filter.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInside = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcInside.set(MDC.get(CorrelationIdFilter.CORRELATION_MDC_KEY));
        filter.doFilter(request, response, chain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_HEADER);
        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
        assertEquals(correlationId, mdcInside.get());
        assertTrue(MDC.getCopyOfContextMap() == null || !MDC.getCopyOfContextMap().containsKey(CorrelationIdFilter.CORRELATION_MDC_KEY));
    }

    @Test
    void shouldReuseProvidedCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInside = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcInside.set(MDC.get(CorrelationIdFilter.CORRELATION_MDC_KEY));
        filter.doFilter(request, response, chain);

        assertEquals("abc-123", response.getHeader(CorrelationIdFilter.CORRELATION_HEADER));
        assertEquals("abc-123", mdcInside.get());
    }

    @Test
    void shouldGenerateWhenProvidedHeaderIsBlankAndTrimAndTruncateWhenTooLong() throws ServletException, IOException {
        MockHttpServletRequest blankRequest = new MockHttpServletRequest();
        blankRequest.addHeader(CorrelationIdFilter.CORRELATION_HEADER, "   ");
        MockHttpServletResponse blankResponse = new MockHttpServletResponse();

        filter.doFilter(blankRequest, blankResponse, (req, res) -> {
            // no-op
        });

        String generated = blankResponse.getHeader(CorrelationIdFilter.CORRELATION_HEADER);
        assertNotNull(generated);
        assertFalse(generated.isBlank());

        String longValue = "x".repeat(200);
        MockHttpServletRequest longRequest = new MockHttpServletRequest();
        longRequest.addHeader(CorrelationIdFilter.CORRELATION_HEADER, " " + longValue + " ");
        MockHttpServletResponse longResponse = new MockHttpServletResponse();

        filter.doFilter(longRequest, longResponse, (req, res) -> {
            // no-op
        });

        String truncated = longResponse.getHeader(CorrelationIdFilter.CORRELATION_HEADER);
        assertNotNull(truncated);
        assertEquals(128, truncated.length());
        assertEquals(longValue.substring(0, 128), truncated);
    }
}

