package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.GeradorNotaFiscalApplication;
import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import br.com.itau.geradornotafiscal.service.exception.BadRequestException;
import br.com.itau.geradornotafiscal.service.exception.IntegracaoNotaFiscalException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
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
    void shouldReturnBadRequestWhenValorTotalItensDivergesFromCalculatedSubtotal() throws Exception {
        when(geradorNotaFiscalService.gerarNotaFiscal(any()))
                .thenThrow(new BadRequestException("valor_total_itens divergente do subtotal calculado. informado=1.00 calculado=100.00"));

        String payload = """
                {
                  "id_pedido": 1,
                  "data": "2022-05-01",
                  "valor_total_itens": 1.0,
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("valor_total_itens divergente")));
    }

    @Test
    void shouldAcceptImmutablePayloadExampleForPessoaFisica() throws Exception {
        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal("nf-pf")
                .data(LocalDateTime.of(2026, 3, 28, 14, 0, 0))
                .valorTotalItens(100.00)
                .valorFrete(10.48)
                .build();

        when(geradorNotaFiscalService.gerarNotaFiscal(any())).thenReturn(notaFiscal);

        String payload = readClasspathPayload("paylods/teste-pf.json");

        mockMvc.perform(post("/api/pedido/gerarNotaFiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_nota_fiscal").value("nf-pf"));
    }

    @Test
    void shouldAcceptImmutablePayloadExampleForPessoaJuridica() throws Exception {
        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal("nf-pj")
                .data(LocalDateTime.of(2026, 3, 28, 14, 0, 0))
                .valorTotalItens(5840.00)
                .valorFrete(75.46)
                .build();

        when(geradorNotaFiscalService.gerarNotaFiscal(any())).thenReturn(notaFiscal);

        String payload = readClasspathPayload("paylods/teste-pj-simples.json");

        mockMvc.perform(post("/api/pedido/gerarNotaFiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_nota_fiscal").value("nf-pj"));
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

    @Test
    void shouldReturnBadRequestWithFieldDetailsWhenPayloadHasInvalidTypes() throws Exception {
        String payload = """
                {
                  "id_pedido": "abc",
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payload JSON invalido"))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("id_pedido")));
    }

    @Test
    void shouldReturnBadRequestWithFieldDetailsWhenPayloadHasInvalidDate() throws Exception {
        String payload = """
                {
                  "id_pedido": 1,
                  "data": "31-12-2026",
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payload JSON invalido"))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("data")));
    }

    @Test
    void shouldReturnBadRequestWithFieldDetailsWhenPayloadHasUnknownField() throws Exception {
        String payload = """
                {
                  "id_pedido": 1,
                  "data": "2022-05-01",
                  "valor_total_itens": 100.0,
                  "valor_frete": 10.0,
                  "campo_desconhecido": "nao permitido",
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payload JSON invalido"))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("campo_desconhecido")));
    }

    @Test
    void shouldReturnBadRequestWhenEnumValueIsInvalid() throws Exception {
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
                    "tipo_pessoa": "PESSOA_X",
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payload JSON invalido"))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("tipo_pessoa")));
    }

    @Test
    void shouldReturnBadRequestWhenEnderecoRequiredFieldIsMissing() throws Exception {
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
                        "finalidade": "ENTREGA"
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/pedido/gerarNotaFiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados de entrada invalidos"))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("regiao")));
    }

    @Test
    void shouldReturnBadRequestWhenEnderecoFinalidadeIsMissing() throws Exception {
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
                        "regiao": "SUDESTE"
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/pedido/gerarNotaFiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados de entrada invalidos"))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("finalidade")));
    }

    private String readClasspathPayload(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
