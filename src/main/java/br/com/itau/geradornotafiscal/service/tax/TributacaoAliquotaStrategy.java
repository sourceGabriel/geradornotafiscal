package br.com.itau.geradornotafiscal.service.tax;

import br.com.itau.geradornotafiscal.model.Destinatario;

import java.math.BigDecimal;

public interface TributacaoAliquotaStrategy {

    boolean supports(Destinatario destinatario);

    BigDecimal calcularAliquota(BigDecimal valorTotalItens);
}

