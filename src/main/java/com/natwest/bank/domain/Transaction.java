package com.natwest.bank.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private String id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private Money amount;

    @Column(nullable = false)
    private Instant timestamp;

    private String description;

    @Column(name = "related_account_id")
    private String relatedAccountId;

    protected Transaction() {
        // JPA
    }

    private Transaction(Builder builder) {
        this.id = builder.id;
        this.accountId = builder.accountId;
        this.type = builder.type;
        this.amount = builder.amount;
        this.timestamp = builder.timestamp;
        this.description = builder.description;
        this.relatedAccountId = builder.relatedAccountId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public TransactionType getType() { return type; }
    public Money getAmount() { return amount; }
    public Instant getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getRelatedAccountId() { return relatedAccountId; }

    public static final class Builder {
        private String id = UUID.randomUUID().toString();
        private String accountId;
        private TransactionType type;
        private Money amount;
        private Instant timestamp = Instant.now();
        private String description = "";
        private String relatedAccountId;

        public Builder id(String id) { this.id = id; return this; }
        public Builder accountId(String accountId) { this.accountId = accountId; return this; }
        public Builder type(TransactionType type) { this.type = type; return this; }
        public Builder amount(Money amount) { this.amount = amount; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder relatedAccountId(String relatedAccountId) { this.relatedAccountId = relatedAccountId; return this; }

        public Transaction build() {
            Objects.requireNonNull(accountId, "accountId is required");
            Objects.requireNonNull(type, "type is required");
            Objects.requireNonNull(amount, "amount is required");
            Objects.requireNonNull(timestamp, "timestamp is required");
            return new Transaction(this);
        }
    }
}
