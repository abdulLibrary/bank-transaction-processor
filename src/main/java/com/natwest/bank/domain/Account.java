package com.natwest.bank.domain;

import com.natwest.bank.exception.InsufficientFundsException;
import com.natwest.bank.exception.InvalidAmountException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root with optimistic locking (@Version).
 * All money-moving operations go through this class to keep invariants intact.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private String id;

    @Column(nullable = false, precision = 19, scale = 2)
    private Money balance;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "accountId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    private List<Transaction> ledger = new ArrayList<>();

    protected Account() {
        // JPA
    }

    public Account(String id, Money initialBalance) {
        this.id = Objects.requireNonNull(id, "Account id cannot be null");
        if (initialBalance == null || initialBalance.isNegative()) {
            throw new InvalidAmountException("Initial balance must be zero or positive");
        }
        this.balance = initialBalance;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.version = 0L;
    }

    public Account(String id) {
        this(id, Money.ZERO);
    }

    public String getId() {
        return id;
    }

    public Money getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Transaction> getLedger() {
        return Collections.unmodifiableList(ledger);
    }

    public void deposit(Money amount, String description) {
        validatePositiveAmount(amount);
        balance = balance.add(amount);
        ledger.add(Transaction.builder()
                .accountId(id)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .description(description)
                .build());
        touch();
    }

    public void withdraw(Money amount, String description) {
        validatePositiveAmount(amount);
        ensureSufficientFunds(amount);
        balance = balance.subtract(amount);
        ledger.add(Transaction.builder()
                .accountId(id)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .description(description)
                .build());
        touch();
    }

    public void transferOut(Money amount, String toAccountId, String description) {
        validatePositiveAmount(amount);
        ensureSufficientFunds(amount);
        balance = balance.subtract(amount);
        ledger.add(Transaction.builder()
                .accountId(id)
                .type(TransactionType.TRANSFER_OUT)
                .amount(amount)
                .relatedAccountId(toAccountId)
                .description(description)
                .build());
        touch();
    }

    public void transferIn(Money amount, String fromAccountId, String description) {
        validatePositiveAmount(amount);
        balance = balance.add(amount);
        ledger.add(Transaction.builder()
                .accountId(id)
                .type(TransactionType.TRANSFER_IN)
                .amount(amount)
                .relatedAccountId(fromAccountId)
                .description(description)
                .build());
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private void validatePositiveAmount(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidAmountException("Amount must be positive: " + amount);
        }
    }

    private void ensureSufficientFunds(Money amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds for account " + id +
                    ". Balance: " + balance + ", requested: " + amount);
        }
    }
}
