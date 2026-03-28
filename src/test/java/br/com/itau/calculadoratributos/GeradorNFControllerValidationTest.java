package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.GeradorNotaFiscalApplication;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
}
