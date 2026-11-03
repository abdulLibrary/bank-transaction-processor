package com.natwest.bank.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bank.outbox.publisher", havingValue = "logging", matchIfMissing = true)
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(String eventType, String aggregateType, String aggregateId, String payload) {
        log.info("OUTBOX PUBLISH | type={} aggregateType={} aggregateId={} payload={}",
                eventType, aggregateType, aggregateId, payload);
    }
}
