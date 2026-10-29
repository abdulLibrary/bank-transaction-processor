package com.natwest.bank.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable money value object. Scale fixed at 2 with HALF_EVEN rounding.
 */
public final class Money implements Comparable<Money> {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    /** Max value compatible with DB DECIMAL(19,2). */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999999999999.99");

    public static final Money ZERO = of(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        Money money = new Money(amount);
        if (money.amount.abs().compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Amount exceeds maximum supported value of " + MAX_AMOUNT.toPlainString());
        }
        return money;
    }

    @JsonCreator
    public static Money of(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("Amount cannot be null or blank");
        }
        try {
            return of(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount);
        }
    }

    public static Money of(double amount) {
        return of(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        return of(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return of(this.amount.subtract(other.amount));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @JsonValue
    public String toPlainString() {
        return amount.toPlainString();
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return toPlainString();
    }

    @Converter(autoApply = true)
    public static class MoneyConverter implements AttributeConverter<Money, BigDecimal> {
        @Override
        public BigDecimal convertToDatabaseColumn(Money money) {
            return money == null ? null : money.getAmount();
        }

        @Override
        public Money convertToEntityAttribute(BigDecimal dbData) {
            return dbData == null ? null : Money.of(dbData);
        }
    }
}