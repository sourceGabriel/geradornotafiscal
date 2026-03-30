package br.com.itau.geradornotafiscal.web.error;

import br.com.itau.geradornotafiscal.service.exception.BadRequestException;
import br.com.itau.geradornotafiscal.service.exception.IntegracaoNotaFiscalException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centraliza o mapeamento de excecoes para respostas HTTP padronizadas da API.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final MeterRegistry meterRegistry;

    public ApiExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        registrarErroHttp("400");
        log.warn("Requisicao invalida: {}", ex.getMessage());
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                List.of()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        registrarErroHttp("400");
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        log.warn("Falha de validacao bean: {}", details);

        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Dados de entrada invalidos",
                details
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        registrarErroHttp("400");
        List<String> details = extractDeserializationDetails(ex);
        log.warn("Payload invalido: {}", details);
        log.debug("Stacktrace de erro de desserializacao", ex);

        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Payload JSON invalido",
                details
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IntegracaoNotaFiscalException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegracao(IntegracaoNotaFiscalException ex) {
        registrarErroHttp("502");
        log.error("Falha em integracao externa: {}", ex.getMessage(), ex);
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_GATEWAY.value(),
                HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                ex.getMessage(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        registrarErroHttp("500");
        log.error("Erro inesperado ao processar requisicao", ex);
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Erro inesperado ao processar requisicao",
                List.of(ex.getClass().getSimpleName())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private List<String> extractDeserializationDetails(HttpMessageNotReadableException ex) {
        Throwable cause = findCause(ex, UnrecognizedPropertyException.class, InvalidFormatException.class, MismatchedInputException.class);

        if (cause == null) {
            cause = ex.getMostSpecificCause();
        }

        if (cause instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
            return List.of("Campo nao permitido: '" + unrecognizedPropertyException.getPropertyName() + "'.");
        }

        if (cause instanceof InvalidFormatException invalidFormatException) {
            String fieldPath = jsonPath(invalidFormatException.getPath());
            String expectedType = describeType(invalidFormatException.getTargetType());
            return List.of("Campo '" + fieldPath + "' recebeu valor invalido '"
                    + invalidFormatException.getValue() + "'. Esperado: " + expectedType + ".");
        }

        if (cause instanceof MismatchedInputException mismatchedInputException) {
            String fieldPath = jsonPath(mismatchedInputException.getPath());
            return List.of("Campo '" + fieldPath + "' esta com formato/tipo invalido.");
        }

        return List.of("Nao foi possivel interpretar o JSON enviado. Verifique tipos e formato dos campos.");
    }

    @SafeVarargs
    private Throwable findCause(Throwable throwable, Class<? extends Throwable>... candidateTypes) {
        Throwable current = throwable;
        while (current != null) {
            for (Class<? extends Throwable> candidateType : candidateTypes) {
                if (candidateType.isInstance(current)) {
                    return current;
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private String jsonPath(List<com.fasterxml.jackson.databind.JsonMappingException.Reference> references) {
        if (references == null || references.isEmpty()) {
            return "payload";
        }

        return references.stream()
                .map(reference -> reference.getFieldName() != null ? reference.getFieldName() : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private String describeType(Class<?> targetType) {
        if (targetType == null) {
            return "tipo valido";
        }

        if (targetType.isEnum()) {
            return "um dos valores: " + Arrays.stream(targetType.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }

        return targetType.getSimpleName();
    }

    private void registrarErroHttp(String status) {
        meterRegistry.counter("nota_fiscal.http.errors", "status", status).increment();
    }
}
