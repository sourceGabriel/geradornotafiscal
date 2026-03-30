package br.com.itau.geradornotafiscal.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Builder
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
/**
 * Representa o payload de entrada do pedido para emissao de nota fiscal.
 */
public class Pedido {
	 @JsonProperty("id_pedido")
	    @NotNull(message = "id_pedido e obrigatorio")
	    @Positive(message = "id_pedido deve ser maior que zero")
	    private Long idPedido;

	    @JsonProperty("data")
	    @NotNull(message = "data e obrigatoria")
	    private LocalDate data;

	    @JsonProperty("valor_total_itens")
	    @PositiveOrZero(message = "valor_total_itens nao pode ser negativo")
	    private double valorTotalItens;

	    @JsonProperty("valor_frete")
	    @PositiveOrZero(message = "valor_frete nao pode ser negativo")
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
