package com.natwest.bank.idempotency;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyRecord.IdempotencyId.class)
public class IdempotencyRecord {

    public enum Status {
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Id
    @Column(length = 64)
    private String operation;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status = Status.PROCESSING;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String operation, String requestHash) {
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
        this.requestHash = requestHash;
        this.status = Status.PROCESSING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markCompleted(int responseStatus, String responseBody) {
        this.status = Status.COMPLETED;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = Status.FAILED;
        this.updatedAt = Instant.now();
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getOperation() { return operation; }
    public String getRequestHash() { return requestHash; }
    public Status getStatus() { return status; }
    public Integer getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

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