package br.com.itau.geradornotafiscal.service.tax;

import br.com.itau.geradornotafiscal.model.Destinatario;
import br.com.itau.geradornotafiscal.model.RegimeTributacaoPJ;
import br.com.itau.geradornotafiscal.model.TipoPessoa;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LucroPresumidoAliquotaStrategy extends AbstractFaixaAliquotaStrategy {

    @Override
    public boolean supports(Destinatario destinatario) {
        return destinatario != null
                && destinatario.getTipoPessoa() == TipoPessoa.JURIDICA
                && destinatario.getRegimeTributacao() == RegimeTributacaoPJ.LUCRO_PRESUMIDO;
    }

    @Override
    public BigDecimal calcularAliquota(BigDecimal valorTotalItens) {
        return porFaixa(valorTotalItens, 1000, 2000, 5000, "0.03", "0.09", "0.16", "0.20");
    }
}

