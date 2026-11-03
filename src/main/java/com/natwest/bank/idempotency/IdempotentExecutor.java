package com.natwest.bank.idempotency;



import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Runs an HTTP command at most once per (Idempotency-Key, operation).
 * Lives at the API edge — not in the domain.
 */
@Component
public class IdempotentExecutor {

    private final IdempotencyService idempotencyService;

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

        Optional<IdempotencyRecord> existing = idempotencyService.find(key, operation);
        if (existing.isPresent()) {
            idempotencyService.assertSameRequest(existing.get(), requestBody);
            return ResponseEntity.status(existing.get().getResponseStatus()).build();
        }

        ResponseEntity<Void> response = action.get();
        idempotencyService.save(key, operation, requestBody, response.getStatusCode().value(), null);
        return response;
    }
}
