package br.com.itau.geradornotafiscal.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class Pedido {
	 @JsonProperty("id_pedido")
	    private int idPedido;

	    @JsonProperty("data")
	    private LocalDate data;

	    @JsonProperty("valor_total_itens")
	    private double valorTotalItens;

	    @JsonProperty("valor_frete")
	    private double valorFrete;

	    @JsonProperty("itens")
	    @NotEmpty(message = "Pedido deve conter ao menos um item")
	    @Valid
	    private List<Item> itens;

	    @JsonProperty("destinatario")
	    @NotNull(message = "Destinatario e obrigatorio")
	    @Valid
	    private Destinatario destinatario;

}
