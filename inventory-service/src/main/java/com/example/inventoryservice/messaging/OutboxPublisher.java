package com.example.inventoryservice.messaging;

import com.example.inventoryservice.entity.OutboxEvent;
import com.example.inventoryservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private static final String PENDING = "PENDING";
    private static final String PUBLISHED = "PUBLISHED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void enqueue(String topic, String messageKey, Object payload) {
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .topic(topic)
                    .messageKey(messageKey)
                    .payloadType(payload.getClass().getName())
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(PENDING)
                    .attempts(0)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save outbox event", exception);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent event : outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(PENDING)) {
            try {
                Object payload = objectMapper.readValue(event.getPayload(), Class.forName(event.getPayloadType()));
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), payload).get(10, TimeUnit.SECONDS);
                event.setStatus(PUBLISHED);
                event.setPublishedAt(Instant.now());
            } catch (Exception exception) {
                event.setAttempts(event.getAttempts() + 1);
                log.warn("Outbox publish failed; eventId={}, attempt={}", event.getId(), event.getAttempts(), exception);
            }
        }
    }
}
