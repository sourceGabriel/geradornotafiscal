package br.com.itau.geradornotafiscal.service.exception;

/**
 * Excecao de falha em integracao externa durante o processamento da nota.
 */
public class IntegracaoNotaFiscalException extends RuntimeException {

    /**
     * Cria excecao com mensagem de erro de integracao.
     *
     * @param message detalhe da falha
     */
    public IntegracaoNotaFiscalException(String message) {
        super(message);
    }

    /**
     * Cria excecao com causa original da falha.
     *
     * @param message detalhe da falha
     * @param cause causa raiz
     */
    public IntegracaoNotaFiscalException(String message, Throwable cause) {
        super(message, cause);
    }
}

