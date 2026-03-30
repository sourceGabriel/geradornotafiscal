package br.com.itau.geradornotafiscal.service.tax;

import br.com.itau.geradornotafiscal.model.Destinatario;
import br.com.itau.geradornotafiscal.model.TipoPessoa;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PessoaFisicaAliquotaStrategy extends AbstractFaixaAliquotaStrategy {

    @Override
    public boolean supports(Destinatario destinatario) {
        return destinatario != null && destinatario.getTipoPessoa() == TipoPessoa.FISICA;
    }

    @Override
    public BigDecimal calcularAliquota(BigDecimal valorTotalItens) {
        return porFaixa(valorTotalItens, 500, 2000, 3500, "0.00", "0.12", "0.15", "0.17");
    }
}

