package br.com.itau.geradornotafiscal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ItemNotaFiscal {
    @JsonProperty("id_item")
    private String idItem;

    @JsonProperty("descricao")
    private String descricao;

    @JsonProperty("valor_unitario")
    private double valorUnitario;

    @JsonProperty("quantidade")
    private int quantidade;

    @JsonProperty("valor_tributo_item")
    private double valorTributoItem;

}