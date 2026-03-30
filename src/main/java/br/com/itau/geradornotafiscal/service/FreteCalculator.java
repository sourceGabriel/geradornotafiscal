package br.com.itau.geradornotafiscal.service;

import br.com.itau.geradornotafiscal.model.Regiao;
import br.com.itau.geradornotafiscal.service.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FreteCalculator {

    private static final BigDecimal MULTIPLICADOR_NORTE = new BigDecimal("1.08");
    private static final BigDecimal MULTIPLICADOR_NORDESTE = new BigDecimal("1.085");
    private static final BigDecimal MULTIPLICADOR_CENTRO_OESTE = new BigDecimal("1.07");
    private static final BigDecimal MULTIPLICADOR_SUDESTE = new BigDecimal("1.048");
    private static final BigDecimal MULTIPLICADOR_SUL = new BigDecimal("1.06");

    public BigDecimal calcular(BigDecimal valorFrete, Regiao regiao) {
        if (valorFrete == null) {
            throw new BadRequestException("valor_frete e obrigatorio");
        }

        if (regiao == null) {
            return valorFrete.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal multiplicador = switch (regiao) {
            case NORTE -> MULTIPLICADOR_NORTE;
            case NORDESTE -> MULTIPLICADOR_NORDESTE;
            case CENTRO_OESTE -> MULTIPLICADOR_CENTRO_OESTE;
            case SUDESTE -> MULTIPLICADOR_SUDESTE;
            case SUL -> MULTIPLICADOR_SUL;
        };

        return valorFrete.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);
    }
}
