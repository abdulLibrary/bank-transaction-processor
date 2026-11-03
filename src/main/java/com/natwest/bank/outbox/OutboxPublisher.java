package com.natwest.bank.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.natwest.bank.event.DomainEvent;
import com.natwest.bank.port.DomainEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transactional outbox writer + poller. Implements DomainEventPort
 * so application services stay decoupled from messaging details.
 */
@Component
public class OutboxPublisher implements DomainEventPort {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final int batchSize;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           ObjectMapper objectMapper,
                           DomainEventPublisher domainEventPublisher,
                           @Value("${bank.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.batchSize = batchSize;
    }

    @Override
    @Transactional
    public void record(String aggregateType, String aggregateId, DomainEvent event) {
        enqueue(aggregateType, aggregateId, event);
    }

    @Transactional
    public void enqueue(String aggregateType, String aggregateId, DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    aggregateType,
                    aggregateId,
                    event.eventType(),
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.debug("Enqueued outbox event {} for aggregate {}", event.eventType(), aggregateId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize domain event", e);
        }
    }

    @Scheduled(fixedDelayString = "${bank.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findPendingEvents();
        int processed = 0;
        for (OutboxEvent event : pending) {
            if (processed >= batchSize) {
                break;
            }
            try {
                domainEventPublisher.publish(
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getPayload()
                );
                event.markPublished();
                outboxRepository.save(event);
                processed++;
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id={}", event.getId(), ex);
                event.markFailed();
                outboxRepository.save(event);
            }
        }
    }
}