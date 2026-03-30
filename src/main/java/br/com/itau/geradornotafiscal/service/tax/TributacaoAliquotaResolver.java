package br.com.itau.geradornotafiscal.service.tax;

import br.com.itau.geradornotafiscal.model.Destinatario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
/**
 * Resolve a aliquota aplicavel selecionando a primeira estrategia compativel com o destinatario.
 */
public class TributacaoAliquotaResolver {

    private final List<TributacaoAliquotaStrategy> strategies;

    public TributacaoAliquotaResolver(List<TributacaoAliquotaStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * Resolve a aliquota por estrategia. Caso nenhuma seja compativel, retorna 0.
     */
    public BigDecimal resolverAliquota(BigDecimal valorTotalItens, Destinatario destinatario) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(destinatario))
                .findFirst()
                .map(strategy -> strategy.calcularAliquota(valorTotalItens))
                .orElse(BigDecimal.ZERO);
    }
}

