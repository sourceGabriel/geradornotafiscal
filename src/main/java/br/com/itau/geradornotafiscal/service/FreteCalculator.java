package br.com.itau.geradornotafiscal.service;

import br.com.itau.geradornotafiscal.model.Regiao;
import br.com.itau.geradornotafiscal.service.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class FreteCalculator {

    private static final Map<Regiao, BigDecimal> MULTIPLICADORES = Map.of(
            Regiao.NORTE, new BigDecimal("1.08"),
            Regiao.NORDESTE, new BigDecimal("1.085"),
            Regiao.CENTRO_OESTE, new BigDecimal("1.07"),
            Regiao.SUDESTE, new BigDecimal("1.048"),
            Regiao.SUL, new BigDecimal("1.06")
    );

    public BigDecimal calcular(BigDecimal valorFrete, Regiao regiao) {
        if (valorFrete == null) {
            throw new BadRequestException("valor_frete e obrigatorio");
        }

        if (regiao == null) {
            return valorFrete.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal multiplicador = MULTIPLICADORES.getOrDefault(regiao, BigDecimal.ONE);
        return valorFrete.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);
    }
}
