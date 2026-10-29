package com.natwest.bank.idempotency;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyRecord.IdempotencyId.class)
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Id
    @Column(length = 64)
    private String operation;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String operation, String requestHash,
                             int responseStatus, String responseBody) {
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getOperation() { return operation; }
    public String getRequestHash() { return requestHash; }
    public int getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; }
    public Instant getCreatedAt() { return createdAt; }

    public static class IdempotencyId implements Serializable {
        private String idempotencyKey;
        private String operation;

        public IdempotencyId() {}
        public IdempotencyId(String idempotencyKey, String operation) {
            this.idempotencyKey = idempotencyKey;
            this.operation = operation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdempotencyId that)) return false;
            return Objects.equals(idempotencyKey, that.idempotencyKey)
                    && Objects.equals(operation, that.operation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idempotencyKey, operation);
        }
    }
}
