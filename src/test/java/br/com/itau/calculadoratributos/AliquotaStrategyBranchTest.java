package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.model.Destinatario;
import br.com.itau.geradornotafiscal.model.RegimeTributacaoPJ;
import br.com.itau.geradornotafiscal.model.TipoPessoa;
import br.com.itau.geradornotafiscal.service.tax.LucroPresumidoAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.LucroRealAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.PessoaFisicaAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.SimplesNacionalAliquotaStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliquotaStrategyBranchTest {

    @Test
    void shouldEvaluateSupportsBranchesForStrategies() {
        SimplesNacionalAliquotaStrategy simples = new SimplesNacionalAliquotaStrategy();
        LucroRealAliquotaStrategy lucroReal = new LucroRealAliquotaStrategy();
        LucroPresumidoAliquotaStrategy lucroPresumido = new LucroPresumidoAliquotaStrategy();
        PessoaFisicaAliquotaStrategy pessoaFisica = new PessoaFisicaAliquotaStrategy();

        assertFalse(simples.supports(null));
        assertFalse(lucroReal.supports(null));
        assertFalse(lucroPresumido.supports(null));
        assertFalse(pessoaFisica.supports(null));

        Destinatario fisica = destinatario(TipoPessoa.FISICA, null);
        assertFalse(simples.supports(fisica));
        assertFalse(lucroReal.supports(fisica));
        assertFalse(lucroPresumido.supports(fisica));
        assertTrue(pessoaFisica.supports(fisica));

        Destinatario simplesNacional = destinatario(TipoPessoa.JURIDICA, RegimeTributacaoPJ.SIMPLES_NACIONAL);
        assertTrue(simples.supports(simplesNacional));
        assertFalse(lucroReal.supports(simplesNacional));
        assertFalse(lucroPresumido.supports(simplesNacional));

        Destinatario lucroRealDestinatario = destinatario(TipoPessoa.JURIDICA, RegimeTributacaoPJ.LUCRO_REAL);
        assertTrue(lucroReal.supports(lucroRealDestinatario));

        Destinatario lucroPresumidoDestinatario = destinatario(TipoPessoa.JURIDICA, RegimeTributacaoPJ.LUCRO_PRESUMIDO);
        assertTrue(lucroPresumido.supports(lucroPresumidoDestinatario));
    }

    @Test
    void shouldCoverFaixaBoundariesForSimplesAndPessoaFisica() {
        SimplesNacionalAliquotaStrategy simples = new SimplesNacionalAliquotaStrategy();
        PessoaFisicaAliquotaStrategy pessoaFisica = new PessoaFisicaAliquotaStrategy();

        assertEquals(new BigDecimal("0.03"), simples.calcularAliquota(new BigDecimal("999.99")));
        assertEquals(new BigDecimal("0.07"), simples.calcularAliquota(new BigDecimal("2000.00")));
        assertEquals(new BigDecimal("0.13"), simples.calcularAliquota(new BigDecimal("5000.00")));
        assertEquals(new BigDecimal("0.19"), simples.calcularAliquota(new BigDecimal("5000.01")));

        assertEquals(new BigDecimal("0.00"), pessoaFisica.calcularAliquota(new BigDecimal("499.99")));
        assertEquals(new BigDecimal("0.12"), pessoaFisica.calcularAliquota(new BigDecimal("500.00")));
        assertEquals(new BigDecimal("0.15"), pessoaFisica.calcularAliquota(new BigDecimal("3500.00")));
        assertEquals(new BigDecimal("0.17"), pessoaFisica.calcularAliquota(new BigDecimal("3500.01")));
    }

    private Destinatario destinatario(TipoPessoa tipoPessoa, RegimeTributacaoPJ regime) {
        Destinatario destinatario = new Destinatario();
        destinatario.setTipoPessoa(tipoPessoa);
        destinatario.setRegimeTributacao(regime);
        return destinatario;
    }
}

