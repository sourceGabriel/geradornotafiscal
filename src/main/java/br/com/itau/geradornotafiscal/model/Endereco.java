package br.com.itau.geradornotafiscal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
/**
 * Endereco do destinatario usado para regras de entrega e frete.
 */
public class Endereco {
    @JsonProperty("cep")
    private String cep;

    @JsonProperty("logradouro")
    private String logradouro;

    @JsonProperty("numero")
    private String numero;

    @JsonProperty("estado")
    private String estado;

    @JsonProperty("bairro")
    private String bairro;

    @JsonProperty("cidade")
    private String cidade;

    @JsonProperty("pais")
    private String pais;

    @JsonProperty("complemento")
    private String complemento;

    @JsonProperty("finalidade")
    @NotNull(message = "finalidade do endereco e obrigatoria")
    private Finalidade finalidade;

    @JsonProperty("regiao")
    @NotNull(message = "regiao do endereco e obrigatoria")
    private Regiao regiao;
}