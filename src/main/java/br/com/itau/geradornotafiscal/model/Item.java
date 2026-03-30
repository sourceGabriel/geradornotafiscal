package br.com.itau.geradornotafiscal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
/**
 * Item do pedido recebido no payload de entrada.
 */
public class Item {
     @JsonProperty("id_item")
     @NotBlank(message = "id_item e obrigatorio")
     private String idItem;

     @JsonProperty("descricao")
     @NotBlank(message = "descricao e obrigatoria")
     private String descricao;

     @JsonProperty("valor_unitario")
     @Positive(message = "valor_unitario deve ser maior que zero")
     private double valorUnitario;

     @JsonProperty("quantidade")
     @Positive(message = "quantidade deve ser maior que zero")
     private int quantidade;

}
