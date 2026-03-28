package br.com.itau.geradornotafiscal.service.tax;

import java.math.BigDecimal;

public abstract class AbstractFaixaAliquotaStrategy implements TributacaoAliquotaStrategy {

    protected BigDecimal porFaixa(BigDecimal valorTotalItens,
                                  int limite1,
                                  int limite2,
                                  int limite3,
                                  String aliquota1,
                                  String aliquota2,
                                  String aliquota3,
                                  String aliquota4) {

        if (valorTotalItens.compareTo(BigDecimal.valueOf(limite1)) < 0) {
            return new BigDecimal(aliquota1);
        }

        if (valorTotalItens.compareTo(BigDecimal.valueOf(limite2)) <= 0) {
            return new BigDecimal(aliquota2);
        }

        if (valorTotalItens.compareTo(BigDecimal.valueOf(limite3)) <= 0) {
            return new BigDecimal(aliquota3);
        }

        return new BigDecimal(aliquota4);
    }
}

