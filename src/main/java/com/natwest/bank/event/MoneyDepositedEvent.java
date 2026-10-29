package com.natwest.bank.event;

import com.natwest.bank.domain.Money;

public class MoneyDepositedEvent extends DomainEvent {

    private final String accountId;
    private final Money amount;

    public MoneyDepositedEvent(String accountId, Money amount) {
        this.accountId = accountId;
        this.amount = amount;
    }

    public String getAccountId() { return accountId; }
    public Money getAmount() { return amount; }

    @Override
    public String eventType() {
        return "MoneyDeposited";
    }
}
