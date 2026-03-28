package br.com.itau.geradornotafiscal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Documento {

    @JsonProperty("numero")
    private String numero;
    @JsonProperty("tipo")
    private TipoDocumento tipo;

}
