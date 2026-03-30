package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.service.exception.BadRequestException;
import br.com.itau.geradornotafiscal.service.exception.IntegracaoNotaFiscalException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExceptionContractsTest {

    @Test
    void shouldCreateBadRequestExceptionWithMessage() {
        BadRequestException ex = new BadRequestException("erro de validacao");
        assertEquals("erro de validacao", ex.getMessage());
    }

    @Test
    void shouldCreateIntegracaoExceptionWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("falha externa");
        IntegracaoNotaFiscalException ex = new IntegracaoNotaFiscalException("erro integracao", cause);

        assertEquals("erro integracao", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}

