package br.com.itau.geradornotafiscal.service;

import br.com.itau.geradornotafiscal.model.Item;
import br.com.itau.geradornotafiscal.model.ItemNotaFiscal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
/**
 * Calcula valor de tributo por item de nota fiscal a partir da aliquota resolvida para o pedido.
 */
public class CalculadoraAliquotaProduto {

    /**
     * Aplica a aliquota sobre o total de cada item (valor_unitario * quantidade).
     *
     * @param items itens do pedido
     * @param aliquotaPercentual aliquota decimal (ex.: 0.15)
     * @return itens da nota fiscal com tributo calculado e arredondado em 2 casas
     */
    public List<ItemNotaFiscal> calcularAliquota(List<Item> items, BigDecimal aliquotaPercentual) {
        List<ItemNotaFiscal> itemNotaFiscalList = new ArrayList<>();

        for (Item item : items) {
            BigDecimal valorUnitario = BigDecimal.valueOf(item.getValorUnitario()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade());
            BigDecimal valorTotalItem = valorUnitario.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
            BigDecimal valorTributo = valorTotalItem.multiply(aliquotaPercentual).setScale(2, RoundingMode.HALF_UP);

            ItemNotaFiscal itemNotaFiscal = ItemNotaFiscal.builder()
                    .idItem(item.getIdItem())
                    .descricao(item.getDescricao())
                    .valorUnitario(valorUnitario.doubleValue())
                    .valorTributoItem(valorTributo.doubleValue())
                    .quantidade(item.getQuantidade())
                    .build();
            itemNotaFiscalList.add(itemNotaFiscal);
        }

        return itemNotaFiscalList;
    }
}
