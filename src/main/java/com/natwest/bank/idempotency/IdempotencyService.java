package com.natwest.bank.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Optional<IdempotencyRecord> find(String key, String operation) {
        return repository.findByIdempotencyKeyAndOperation(key, operation);
    }

    @Transactional
    public void save(String key, String operation, Object requestBody,
                     int responseStatus, Object responseBody) {
        String requestHash = hash(requestBody);
        String responseJson = serialize(responseBody);
        IdempotencyRecord record = new IdempotencyRecord(
                key, operation, requestHash, responseStatus, responseJson);
        repository.save(record);
    }

    public void assertSameRequest(IdempotencyRecord existing, Object currentRequest) {
        String currentHash = hash(currentRequest);
        if (!existing.getRequestHash().equals(currentHash)) {
            throw new IllegalArgumentException(
                    "Idempotency-Key reused with a different request payload");
        }
    }

    private String hash(Object body) {
        try {
            String json = body == null ? "" : objectMapper.writeValueAsString(body);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash request", e);
        }
    }

    private String serialize(Object body) {
        try {
            return body == null ? null : objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return null;
        }
    }
}
