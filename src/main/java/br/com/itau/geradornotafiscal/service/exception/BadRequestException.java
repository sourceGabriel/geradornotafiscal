package br.com.itau.geradornotafiscal.service.exception;

/**
 * Excecao de regra/validacao que deve resultar em HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    /**
     * Cria excecao com mensagem de validacao.
     *
     * @param message detalhe da invalidacao
     */
    public BadRequestException(String message) {
        super(message);
    }
}

