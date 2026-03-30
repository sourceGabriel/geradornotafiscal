package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.model.Regiao;
import br.com.itau.geradornotafiscal.service.FreteCalculator;
import br.com.itau.geradornotafiscal.service.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FreteCalculatorTest {

    private final FreteCalculator freteCalculator = new FreteCalculator();

    @Test
    void shouldThrowBadRequestWhenValorFreteIsNull() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> freteCalculator.calcular(null, Regiao.SUDESTE));

        assertEquals("valor_frete e obrigatorio", exception.getMessage());
    }

    @Test
    void shouldReturnSameFreteWhenRegiaoIsNull() {
        BigDecimal result = freteCalculator.calcular(new BigDecimal("76"), null);

        assertEquals(new BigDecimal("76.00"), result);
    }

    @Test
    void shouldApplyNorthMultiplier() {
        BigDecimal result = freteCalculator.calcular(new BigDecimal("100"), Regiao.NORTE);
        assertEquals(new BigDecimal("108.00"), result);
    }

    @Test
    void shouldApplyNortheastMultiplier() {
        BigDecimal result = freteCalculator.calcular(new BigDecimal("100"), Regiao.NORDESTE);
        assertEquals(new BigDecimal("108.50"), result);
    }

    @Test
    void shouldApplyCenterWestMultiplier() {
        BigDecimal result = freteCalculator.calcular(new BigDecimal("100"), Regiao.CENTRO_OESTE);
        assertEquals(new BigDecimal("107.00"), result);
    }

    @Test
    void shouldApplySoutheastMultiplier() {
        BigDecimal result = freteCalculator.calcular(new BigDecimal("100"), Regiao.SUDESTE);
        assertEquals(new BigDecimal("104.80"), result);
    }

    @Test
    void shouldApplySouthMultiplier() {
        BigDecimal result = freteCalculator.calcular(new BigDecimal("100"), Regiao.SUL);
        assertEquals(new BigDecimal("106.00"), result);
    }
}

