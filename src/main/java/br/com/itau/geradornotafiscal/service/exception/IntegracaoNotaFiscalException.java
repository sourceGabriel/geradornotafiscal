package br.com.itau.geradornotafiscal.service.exception;

public class IntegracaoNotaFiscalException extends RuntimeException {

    public IntegracaoNotaFiscalException(String message) {
        super(message);
    }

    public IntegracaoNotaFiscalException(String message, Throwable cause) {
        super(message, cause);
    }
}

