package com.natwest.bank.event;

import com.natwest.bank.domain.Money;

public class TransferCompletedEvent extends DomainEvent {

    private final String fromAccountId;
    private final String toAccountId;
    private final Money amount;

    public TransferCompletedEvent(String fromAccountId, String toAccountId, Money amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    public String getFromAccountId() { return fromAccountId; }
    public String getToAccountId() { return toAccountId; }
    public Money getAmount() { return amount; }

    @Override
    public String eventType() {
        return "TransferCompleted";
    }
}
