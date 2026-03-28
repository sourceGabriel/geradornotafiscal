package br.com.itau.geradornotafiscal.service;

import br.com.itau.geradornotafiscal.model.Item;
import br.com.itau.geradornotafiscal.model.ItemNotaFiscal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class CalculadoraAliquotaProduto {

    public List<ItemNotaFiscal> calcularAliquota(List<Item> items, BigDecimal aliquotaPercentual) {
        List<ItemNotaFiscal> itemNotaFiscalList = new ArrayList<>();

        for (Item item : items) {
            BigDecimal valorUnitario = BigDecimal.valueOf(item.getValorUnitario());
            BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade());
            BigDecimal valorTotalItem = valorUnitario.multiply(quantidade);
            BigDecimal valorTributo = valorTotalItem.multiply(aliquotaPercentual).setScale(2, RoundingMode.HALF_UP);

            ItemNotaFiscal itemNotaFiscal = ItemNotaFiscal.builder()
                    .idItem(item.getIdItem())
                    .descricao(item.getDescricao())
                    .valorUnitario(valorUnitario.setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .quantidade(item.getQuantidade())
                    .valorTributoItem(valorTributo.doubleValue())
                    .build();
            itemNotaFiscalList.add(itemNotaFiscal);
        }

        return itemNotaFiscalList;
    }
}
