package br.com.itau.geradornotafiscal.service;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;

/**
 * Caso de uso principal para processamento de pedido e geracao de nota fiscal.
 */
public interface GeradorNotaFiscalService {

	/**
	 * Processa um pedido e retorna a nota fiscal gerada.
	 * <p>
	 * A implementacao deve validar consistencia de entrada, aplicar regras de calculo,
	 * executar integracoes simuladas e garantir idempotencia por payload.
	 *
	 * @param pedido pedido recebido no contrato de entrada
	 * @return nota fiscal processada
	 */
	NotaFiscal gerarNotaFiscal(Pedido pedido);
}
