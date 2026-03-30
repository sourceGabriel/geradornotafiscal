package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.Destinatario;
import br.com.itau.geradornotafiscal.model.Endereco;
import br.com.itau.geradornotafiscal.model.Finalidade;
import br.com.itau.geradornotafiscal.model.Item;
import br.com.itau.geradornotafiscal.model.ItemNotaFiscal;
import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.model.Regiao;
import br.com.itau.geradornotafiscal.service.CalculadoraAliquotaProduto;
import br.com.itau.geradornotafiscal.service.FreteCalculator;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import br.com.itau.geradornotafiscal.service.exception.BadRequestException;
import br.com.itau.geradornotafiscal.service.idempotency.NotaFiscalIdempotencyStore;
import br.com.itau.geradornotafiscal.service.idempotency.PedidoIdempotencyKeyGenerator;
import br.com.itau.geradornotafiscal.service.tax.TributacaoAliquotaResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra o fluxo de emissao de nota fiscal:
 * validacao, calculo de tributos/frete, idempotencia e integracoes externas simuladas.
 */
@Service
public class GeradorNotaFiscalServiceImpl implements GeradorNotaFiscalService {

    private final CalculadoraAliquotaProduto calculadoraAliquotaProduto;
    private final TributacaoAliquotaResolver tributacaoAliquotaResolver;
    private final FreteCalculator freteCalculator;
    private final NotaFiscalIntegracaoFacade notaFiscalIntegracaoFacade;
    private final NotaFiscalIdempotencyStore idempotencyStore;
    private final PedidoIdempotencyKeyGenerator idempotencyKeyGenerator;

    public GeradorNotaFiscalServiceImpl(CalculadoraAliquotaProduto calculadoraAliquotaProduto,
                                        TributacaoAliquotaResolver tributacaoAliquotaResolver,
                                        FreteCalculator freteCalculator,
                                        NotaFiscalIntegracaoFacade notaFiscalIntegracaoFacade,
                                        NotaFiscalIdempotencyStore idempotencyStore,
                                        PedidoIdempotencyKeyGenerator idempotencyKeyGenerator) {
        this.calculadoraAliquotaProduto = calculadoraAliquotaProduto;
        this.tributacaoAliquotaResolver = tributacaoAliquotaResolver;
        this.freteCalculator = freteCalculator;
        this.notaFiscalIntegracaoFacade = notaFiscalIntegracaoFacade;
        this.idempotencyStore = idempotencyStore;
        this.idempotencyKeyGenerator = idempotencyKeyGenerator;
    }

    /**
     * Executa o fluxo principal com validacao financeira e deduplicacao por idempotencia.
     */
    @Override
    public NotaFiscal gerarNotaFiscal(Pedido pedido) {
        BigDecimal subtotal = validarPedidoERetornarSubtotal(pedido);

        String idempotencyKey = idempotencyKeyGenerator.generate(pedido);
        return idempotencyStore.execute(idempotencyKey, () -> gerarNovaNotaFiscal(pedido, subtotal));
    }

    /**
     * Monta a nota fiscal com base em subtotal validado e regras de negocio aplicadas.
     */
    private NotaFiscal gerarNovaNotaFiscal(Pedido pedido, BigDecimal subtotal) {
        List<Item> itensPedido = pedido.getItens();

        Destinatario destinatario = pedido.getDestinatario();
        BigDecimal aliquota = tributacaoAliquotaResolver.resolverAliquota(subtotal, destinatario);
        List<ItemNotaFiscal> itemNotaFiscalList = calculadoraAliquotaProduto.calcularAliquota(itensPedido, aliquota);

        Regiao regiaoEntrega = encontrarRegiaoEntrega(destinatario);
        BigDecimal valorFreteAjustado = freteCalculator.calcular(BigDecimal.valueOf(pedido.getValorFrete()), regiaoEntrega);

        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal(UUID.randomUUID().toString())
                .data(LocalDateTime.now())
                .valorTotalItens(subtotal.doubleValue())
                .valorFrete(valorFreteAjustado.doubleValue())
                .itens(itemNotaFiscalList)
                .destinatario(destinatario)
                .build();

        notaFiscalIntegracaoFacade.executarIntegracoes(notaFiscal);
        return notaFiscal;
    }

    /**
     * Valida campos obrigatorios de negocio e consistencia monetaria do pedido.
     *
     * @return subtotal calculado a partir dos itens, em escala monetaria de 2 casas
     */
    private BigDecimal validarPedidoERetornarSubtotal(Pedido pedido) {
        if (pedido == null) {
            throw new BadRequestException("Pedido e obrigatorio");
        }

        if (pedido.getDestinatario() == null) {
            throw new BadRequestException("Destinatario e obrigatorio");
        }

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new BadRequestException("Pedido deve conter ao menos um item");
        }

        if (pedido.getDestinatario().getTipoPessoa() == null) {
            throw new BadRequestException("tipo_pessoa e obrigatorio");
        }

        BigDecimal subtotalCalculado = calcularSubtotal(pedido.getItens());
        BigDecimal totalInformado = BigDecimal.valueOf(pedido.getValorTotalItens()).setScale(2, RoundingMode.HALF_UP);

        if (totalInformado.compareTo(subtotalCalculado) != 0) {
            throw new BadRequestException(
                    "valor_total_itens divergente do subtotal calculado. informado="
                            + totalInformado + " calculado=" + subtotalCalculado
            );
        }

        return subtotalCalculado;
    }

    /**
     * Calcula o subtotal financeiro real com base em valor unitario e quantidade de cada item.
     */
    private BigDecimal calcularSubtotal(List<Item> itensPedido) {
        return itensPedido.stream()
                .map(item -> BigDecimal.valueOf(item.getValorUnitario())
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Resolve a regiao de entrega valida para aplicacao do calculo de frete.
     */
    private Regiao encontrarRegiaoEntrega(Destinatario destinatario) {
        if (destinatario == null || destinatario.getEnderecos() == null) {
            throw new BadRequestException("Destinatario deve conter endereco de entrega com regiao");
        }

        return destinatario.getEnderecos().stream()
                .map(Endereco::getRegiao)
                .filter(regiao -> regiao != null)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Endereco de entrega com regiao e obrigatorio"));
    }
}