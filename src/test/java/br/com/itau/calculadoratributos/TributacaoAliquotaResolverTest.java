package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.model.Destinatario;
import br.com.itau.geradornotafiscal.model.RegimeTributacaoPJ;
import br.com.itau.geradornotafiscal.model.TipoPessoa;
import br.com.itau.geradornotafiscal.service.tax.LucroPresumidoAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.LucroRealAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.PessoaFisicaAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.SimplesNacionalAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.TributacaoAliquotaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TributacaoAliquotaResolverTest {

    private TributacaoAliquotaResolver resolver;

    @BeforeEach
    void setup() {
        resolver = new TributacaoAliquotaResolver(List.of(
                new PessoaFisicaAliquotaStrategy(),
                new SimplesNacionalAliquotaStrategy(),
                new LucroRealAliquotaStrategy(),
                new LucroPresumidoAliquotaStrategy()
        ));
    }

    @Test
    void shouldResolveAliquotaForPessoaFisica() {
        Destinatario destinatario = new Destinatario();
        destinatario.setTipoPessoa(TipoPessoa.FISICA);

        BigDecimal aliquota = resolver.resolverAliquota(new BigDecimal("2500"), destinatario);

        assertEquals(new BigDecimal("0.15"), aliquota);
    }

    @Test
    void shouldResolveAliquotaForPessoaJuridicaLucroReal() {
        Destinatario destinatario = new Destinatario();
        destinatario.setTipoPessoa(TipoPessoa.JURIDICA);
        destinatario.setRegimeTributacao(RegimeTributacaoPJ.LUCRO_REAL);

        BigDecimal aliquota = resolver.resolverAliquota(new BigDecimal("5200"), destinatario);

        assertEquals(new BigDecimal("0.20"), aliquota);
    }

    @Test
    void shouldReturnZeroWhenNoStrategyMatches() {
        Destinatario destinatario = new Destinatario();
        destinatario.setTipoPessoa(TipoPessoa.JURIDICA);
        destinatario.setRegimeTributacao(RegimeTributacaoPJ.OUTROS);

        BigDecimal aliquota = resolver.resolverAliquota(new BigDecimal("1000"), destinatario);

        assertEquals(BigDecimal.ZERO, aliquota);
    }
}

