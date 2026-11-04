package com.natwest.bank.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
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
    public Optional<IdempotencyRecord> tryClaim(String key, String operation, Object requestBody) {
        String requestHash = hash(requestBody);
        IdempotencyRecord claim = new IdempotencyRecord(key, operation, requestHash);
        try {
            repository.saveAndFlush(claim);
            return Optional.of(claim);
        } catch (DataIntegrityViolationException ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            if (isConstraintViolation(ex)) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    @Transactional
    public void markCompleted(IdempotencyRecord record, int responseStatus, Object responseBody) {
        record.markCompleted(responseStatus, serialize(responseBody));
        repository.save(record);
    }

    @Transactional
    public void markFailed(IdempotencyRecord record) {
        record.markFailed();
        repository.save(record);
    }

    @Transactional
    public void removeFailed(IdempotencyRecord record) {
        if (record.getStatus() == IdempotencyRecord.Status.FAILED) {
            repository.delete(record);
        }
    }

    @Transactional
    public void save(String key, String operation, Object requestBody,
                     int responseStatus, Object responseBody) {
        String requestHash = hash(requestBody);
        IdempotencyRecord record = new IdempotencyRecord(key, operation, requestHash);
        record.markCompleted(responseStatus, serialize(responseBody));
        try {
            repository.save(record);
        } catch (DataIntegrityViolationException ignored) {
            // concurrent writer already persisted
        }
    }

    public void assertSameRequest(IdempotencyRecord existing, Object currentRequest) {
        String currentHash = hash(currentRequest);
        if (!existing.getRequestHash().equals(currentHash)) {
            throw new IllegalArgumentException(
                    "Idempotency-Key reused with a different request payload");
        }
    }

    private static boolean isConstraintViolation(Throwable ex) {
        while (ex != null) {
            if (ex instanceof DataIntegrityViolationException) {
                return true;
            }
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")
                    || msg.contains("Unique index") || msg.contains("CONSTRAINT"))) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
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