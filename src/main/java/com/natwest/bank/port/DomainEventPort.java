package com.natwest.bank.port;

import com.natwest.bank.event.DomainEvent;

/**
 * Application port for recording domain events (e.g. transactional outbox).
 * Implementations must participate in the caller's transaction when required.
 */
public interface DomainEventPort {

    void record(String aggregateType, String aggregateId, DomainEvent event);
}
