package br.com.itau.calculadoratributos;

import br.com.itau.geradornotafiscal.model.Destinatario;
import br.com.itau.geradornotafiscal.model.Endereco;
import br.com.itau.geradornotafiscal.model.Finalidade;
import br.com.itau.geradornotafiscal.model.Item;
import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.model.Regiao;
import br.com.itau.geradornotafiscal.model.RegimeTributacaoPJ;
import br.com.itau.geradornotafiscal.model.TipoPessoa;
import br.com.itau.geradornotafiscal.port.out.EntregaIntegrationPort;
import br.com.itau.geradornotafiscal.service.CalculadoraAliquotaProduto;
import br.com.itau.geradornotafiscal.service.FreteCalculator;
import br.com.itau.geradornotafiscal.service.impl.GeradorNotaFiscalServiceImpl;
import br.com.itau.geradornotafiscal.service.impl.NotaFiscalIntegracaoFacade;
import br.com.itau.geradornotafiscal.service.tax.LucroPresumidoAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.LucroRealAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.PessoaFisicaAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.SimplesNacionalAliquotaStrategy;
import br.com.itau.geradornotafiscal.service.tax.TributacaoAliquotaResolver;
import br.com.itau.geradornotafiscal.service.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GeradorNotaFiscalServiceImplTest {

    private GeradorNotaFiscalServiceImpl geradorNotaFiscalService;

    @BeforeEach
    void setup() {
        TributacaoAliquotaResolver resolver = new TributacaoAliquotaResolver(List.of(
                new PessoaFisicaAliquotaStrategy(),
                new SimplesNacionalAliquotaStrategy(),
                new LucroRealAliquotaStrategy(),
                new LucroPresumidoAliquotaStrategy()
        ));

        geradorNotaFiscalService = new GeradorNotaFiscalServiceImpl(
                new CalculadoraAliquotaProduto(),
                resolver,
                new FreteCalculator(),
                mock(NotaFiscalIntegracaoFacade.class)
        );
    }

    @Test
    void shouldNotAccumulateItemsBetweenConsecutiveExecutions() {
        Pedido primeiroPedido = criarPedido(TipoPessoa.FISICA, null, List.of(
                criarItem("1", 100.0, 1),
                criarItem("2", 50.0, 1)
        ), 10.0);

        Pedido segundoPedido = criarPedido(TipoPessoa.FISICA, null, List.of(
                criarItem("3", 80.0, 1)
        ), 10.0);

        NotaFiscal primeiraNota = geradorNotaFiscalService.gerarNotaFiscal(primeiroPedido);
        NotaFiscal segundaNota = geradorNotaFiscalService.gerarNotaFiscal(segundoPedido);

        assertEquals(2, primeiraNota.getItens().size());
        assertEquals(1, segundaNota.getItens().size());
        assertEquals("3", segundaNota.getItens().get(0).getIdItem());
    }

    @Test
    void shouldCalculateConsistentTotalsAndTaxesIgnoringInconsistentPedidoTotal() {
        Pedido pedido = criarPedido(TipoPessoa.JURIDICA, RegimeTributacaoPJ.LUCRO_PRESUMIDO, List.of(
                criarItem("a", 1000.0, 3),
                criarItem("b", 500.0, 6)
        ), 100.0);

        // valor de entrada inconsistente proposital para garantir consistencia da nota calculada.
        pedido.setValorTotalItens(1.0);

        NotaFiscal notaFiscal = geradorNotaFiscalService.gerarNotaFiscal(pedido);

        assertEquals(6000.00, notaFiscal.getValorTotalItens(), 0.001);
        assertEquals(104.80, notaFiscal.getValorFrete(), 0.001);
        assertEquals(600.00, notaFiscal.getItens().get(0).getValorTributoItem(), 0.001);
        assertEquals(600.00, notaFiscal.getItens().get(1).getValorTributoItem(), 0.001);

        double totalTributos = notaFiscal.getItens().stream().mapToDouble(item -> item.getValorTributoItem()).sum();
        assertEquals(1200.00, totalTributos, 0.001);
    }

    @Test
    void shouldApplyAdditionalLatencyOnlyForRealLargeItemSets() {
        EntregaIntegrationPort integrationPort = new EntregaIntegrationPort();

        NotaFiscal notaComSeteItens = NotaFiscal.builder().itens(criarItensNota(7)).build();
        NotaFiscal notaComDoisItens = NotaFiscal.builder().itens(criarItensNota(2)).build();

        long inicioGrande = System.currentTimeMillis();
        integrationPort.criarAgendamentoEntrega(notaComSeteItens);
        long duracaoGrande = System.currentTimeMillis() - inicioGrande;

        long inicioPequeno = System.currentTimeMillis();
        integrationPort.criarAgendamentoEntrega(notaComDoisItens);
        long duracaoPequeno = System.currentTimeMillis() - inicioPequeno;

        assertTrue(duracaoGrande >= 850, "Pedido com 7+ itens deve preservar latencia simulada adicional");
        assertTrue(duracaoPequeno < 650, "Pedido pequeno nao deve herdar penalidade de execucoes anteriores");
    }

    @Test
    void shouldThrowBadRequestWhenDeliveryRegionIsMissing() {
        Destinatario destinatario = new Destinatario();
        destinatario.setTipoPessoa(TipoPessoa.FISICA);

        Endereco enderecoSemRegiao = new Endereco();
        enderecoSemRegiao.setFinalidade(Finalidade.ENTREGA);
        enderecoSemRegiao.setRegiao(null);
        destinatario.setEnderecos(List.of(enderecoSemRegiao));

        Pedido pedido = new Pedido();
        pedido.setDestinatario(destinatario);
        pedido.setValorFrete(10.0);
        pedido.setItens(List.of(criarItem("1", 100.0, 1)));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> geradorNotaFiscalService.gerarNotaFiscal(pedido)
        );

        assertTrue(exception.getMessage().contains("Endereco de entrega com regiao e obrigatorio"));
    }

    private Pedido criarPedido(TipoPessoa tipoPessoa, RegimeTributacaoPJ regime, List<Item> itens, double frete) {
        Destinatario destinatario = new Destinatario();
        destinatario.setTipoPessoa(tipoPessoa);
        destinatario.setRegimeTributacao(regime);
        destinatario.setEnderecos(List.of(criarEnderecoEntrega()));

        Pedido pedido = new Pedido();
        pedido.setItens(itens);
        pedido.setValorFrete(frete);
        pedido.setValorTotalItens(99999.0);
        pedido.setDestinatario(destinatario);
        return pedido;
    }

    private Endereco criarEnderecoEntrega() {
        Endereco endereco = new Endereco();
        endereco.setFinalidade(Finalidade.ENTREGA);
        endereco.setRegiao(Regiao.SUDESTE);
        return endereco;
    }

    private Item criarItem(String idItem, double valorUnitario, int quantidade) {
        Item item = new Item();
        item.setIdItem(idItem);
        item.setDescricao("item-" + idItem);
        item.setValorUnitario(valorUnitario);
        item.setQuantidade(quantidade);
        return item;
    }

    private List<br.com.itau.geradornotafiscal.model.ItemNotaFiscal> criarItensNota(int quantidade) {
        List<br.com.itau.geradornotafiscal.model.ItemNotaFiscal> itens = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            itens.add(br.com.itau.geradornotafiscal.model.ItemNotaFiscal.builder()
                    .idItem("id-" + i)
                    .descricao("item")
                    .valorUnitario(10.0)
                    .quantidade(1)
                    .valorTributoItem(1.0)
                    .build());
        }
        return itens;
    }
}