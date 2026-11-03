package com.natwest.bank.outbox;

public interface DomainEventPublisher {
    void publish(String eventType, String aggregateType, String aggregateId, String payload);
}
