package br.com.itau.geradornotafiscal.service.tax;

import br.com.itau.geradornotafiscal.model.Destinatario;

import java.math.BigDecimal;

/**
 * Contrato para estrategias de calculo de aliquota conforme perfil tributario do destinatario.
 */
public interface TributacaoAliquotaStrategy {

    /**
     * Indica se a estrategia atende o destinatario atual.
     */
    boolean supports(Destinatario destinatario);

    /**
     * Calcula aliquota (valor decimal) para o total de itens informado.
     */
    BigDecimal calcularAliquota(BigDecimal valorTotalItens);
}

