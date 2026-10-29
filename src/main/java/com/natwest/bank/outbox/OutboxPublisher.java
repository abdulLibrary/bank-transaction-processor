package com.natwest.bank.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.natwest.bank.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Simple transactional outbox publisher.
 * In production this would publish to Kafka / SNS / EventBridge.
 * Here we log the event (simulating the notification side-effect).
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
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
        for (OutboxEvent event : pending) {
            try {
                // Simulate external notification (e.g. Kafka publish)
                log.info("OUTBOX PUBLISH | type={} aggregateId={} payload={}",
                        event.getEventType(), event.getAggregateId(), event.getPayload());
                event.markPublished();
                outboxRepository.save(event);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id={}", event.getId(), ex);
                event.markFailed();
                outboxRepository.save(event);
            }
        }
    }
}
