package br.com.itau.geradornotafiscal.web.error;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato padronizado de resposta de erro retornado pela API.
 *
 * @param timestamp data/hora do erro
 * @param status status HTTP
 * @param error nome textual do status
 * @param message descricao resumida da falha
 * @param details lista de detalhes de validacao/processamento
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
}

