package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.GeradorNotaFiscalApplication;
import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import br.com.itau.geradornotafiscal.service.exception.IntegracaoNotaFiscalException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GeradorNotaFiscalApplication.class)
@AutoConfigureMockMvc
class GeradorNFControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeradorNotaFiscalService geradorNotaFiscalService;

    @Test
    void shouldReturnBadRequestWhenPayloadIsMissingRequiredFields() throws Exception {
        String payload = """
                {
                  "id_pedido": 1,
                  "valor_frete": 10.0,
                  "itens": [],
                  "destinatario": null
                }
                """;

        mockMvc.perform(post("/api/pedido/gerarNotaFiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados de entrada invalidos"));
    }

    @Test
    void shouldReturnNotaFiscalWhenPayloadIsValid() throws Exception {
        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal("nf-123")
                .data(LocalDateTime.of(2026, 3, 28, 14, 0, 0))
                .valorTotalItens(100.00)
                .valorFrete(10.48)
                .build();

        when(geradorNotaFiscalService.gerarNotaFiscal(any())).thenReturn(notaFiscal);

        String payload = """
                {
                  "id_pedido": 1,
                  "data": "2022-05-01",
                  "valor_total_itens": 100.0,
                  "valor_frete": 10.0,
                  "itens": [
                    {
                      "id_item": "1",
                      "descricao": "Teclado USB",
                      "valor_unitario": 50.0,
                      "quantidade": 2
                    }
                  ],
                  "destinatario": {
                    "nome": "John Doe",
                    "tipo_pessoa": "FISICA",
                    "enderecos": [
                      {
                        "logradouro": "Av do estado",
                        "numero": "5533",
                        "estado": "SP",
                        "cep": "03105003",
                        "finalidade": "ENTREGA",
                        "regiao": "SUDESTE"
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/pedido/gerarNotaFiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_nota_fiscal").value("nf-123"))
                .andExpect(jsonPath("$.valor_total_itens").value(100.0));
    }

    @Test
    void shouldReturnBadGatewayWhenIntegrationFails() throws Exception {
        when(geradorNotaFiscalService.gerarNotaFiscal(any()))
                .thenThrow(new IntegracaoNotaFiscalException("Falha ao executar integracoes da nota fiscal: entrega"));

        String payload = """
                {
                  "id_pedido": 1,
                  "data": "2022-05-01",
                  "valor_total_itens": 100.0,
                  "valor_frete": 10.0,
                  "itens": [
                    {
                      "id_item": "1",
                      "descricao": "Teclado USB",
                      "valor_unitario": 50.0,
                      "quantidade": 2
                    }
                  ],
                  "destinatario": {
                    "nome": "John Doe",
                    "tipo_pessoa": "FISICA",
                    "enderecos": [
                      {
                        "logradouro": "Av do estado",
                        "numero": "5533",
                        "estado": "SP",
                        "cep": "03105003",
                        "finalidade": "ENTREGA",
                        "regiao": "SUDESTE"
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/pedido/gerarNotaFiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Falha ao executar integracoes da nota fiscal: entrega"));
    }
}
