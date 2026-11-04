package com.natwest.bank.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Production-grade idempotent executor.
 *
 * - DB unique constraint = source of truth across instances
 * - ConcurrentHashMap&lt;CompletableFuture&gt; = share in-flight work inside one JVM
 * - Never holds a lock while the business transaction runs
 */
@Component
public class IdempotentExecutor {

    private static final Logger log = LoggerFactory.getLogger(IdempotentExecutor.class);
    private static final long IN_FLIGHT_WAIT_SECONDS = 30;

    private final IdempotencyService idempotencyService;
    private final ConcurrentHashMap<String, CompletableFuture<ResponseEntity<Void>>> inFlight =
            new ConcurrentHashMap<>();

    public IdempotentExecutor(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    public ResponseEntity<Void> execute(String operation,
                                        String key,
                                        Object requestBody,
                                        Supplier<ResponseEntity<Void>> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }

        final String mapKey = operation + ":" + key;

        Optional<IdempotencyRecord> existing = idempotencyService.find(key, operation);
        if (existing.isPresent()) {
            return handleExisting(existing.get(), requestBody);
        }

        CompletableFuture<ResponseEntity<Void>> future = new CompletableFuture<>();
        CompletableFuture<ResponseEntity<Void>> previous = inFlight.putIfAbsent(mapKey, future);

        if (previous != null) {
            return awaitInFlight(previous, key, operation, requestBody);
        }

        try {
            ResponseEntity<Void> response = claimAndExecute(operation, key, requestBody, action);
            future.complete(response);
            return response;
        } catch (RuntimeException ex) {
            future.completeExceptionally(ex);
            throw ex;
        } finally {
            inFlight.remove(mapKey, future);
        }
    }

    private ResponseEntity<Void> claimAndExecute(String operation,
                                                 String key,
                                                 Object requestBody,
                                                 Supplier<ResponseEntity<Void>> action) {
        Optional<IdempotencyRecord> claim = idempotencyService.tryClaim(key, operation, requestBody);

        if (claim.isEmpty()) {
            IdempotencyRecord winner = idempotencyService.find(key, operation)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency key claimed but record not found: " + key));
            return handleExisting(winner, requestBody);
        }

        IdempotencyRecord record = claim.get();
        try {
            ResponseEntity<Void> response = action.get();
            idempotencyService.markCompleted(record, response.getStatusCode().value(), null);
            log.debug("Idempotent {} completed for key={}", operation, key);
            return response;
        } catch (RuntimeException ex) {
            idempotencyService.markFailed(record);
            log.warn("Idempotent {} failed for key={}: {}", operation, key, ex.getMessage());
            throw ex;
        }
    }

    private ResponseEntity<Void> awaitInFlight(CompletableFuture<ResponseEntity<Void>> future,
                                               String key,
                                               String operation,
                                               Object requestBody) {
        try {
            return future.get(IN_FLIGHT_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("In-flight wait ended for key={}, falling back to DB", key);
            return idempotencyService.find(key, operation)
                    .map(rec -> handleExisting(rec, requestBody))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                            .header("Retry-After", "1")
                            .build());
        }
    }

    private ResponseEntity<Void> handleExisting(IdempotencyRecord existing, Object requestBody) {
        if (existing.getStatus() == IdempotencyRecord.Status.FAILED) {
            idempotencyService.removeFailed(existing);
            throw new IllegalStateException(
                    "Previous attempt for this Idempotency-Key failed; please retry the request");
        }

        idempotencyService.assertSameRequest(existing, requestBody);

        return switch (existing.getStatus()) {
            case COMPLETED -> {
                int status = existing.getResponseStatus() != null
                        ? existing.getResponseStatus()
                        : HttpStatus.NO_CONTENT.value();
                yield ResponseEntity.status(status).build();
            }
            case PROCESSING -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("Retry-After", "1")
                    .build();
            case FAILED -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("Retry-After", "0")
                    .build();
        };
    }
}