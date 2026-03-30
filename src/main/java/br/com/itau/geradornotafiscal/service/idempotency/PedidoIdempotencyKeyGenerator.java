package br.com.itau.geradornotafiscal.service.idempotency;

import br.com.itau.geradornotafiscal.model.Pedido;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Gera chave idempotente deterministica a partir do payload canonico do pedido.
 */
@Component
public class PedidoIdempotencyKeyGenerator {

    private final ObjectMapper canonicalMapper;

    public PedidoIdempotencyKeyGenerator() {
        this.canonicalMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    /**
     * Serializa o pedido de forma canonica e aplica SHA-256 para formar a chave.
     */
    public String generate(Pedido pedido) {
        try {
            String canonicalPayload = canonicalMapper.writeValueAsString(pedido);
            return sha256Hex(canonicalPayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nao foi possivel gerar chave de idempotencia", e);
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash indisponivel", e);
        }
    }
}
