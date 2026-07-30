package com.example.microserviceecom.messaging.producer;

import com.example.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedEventKafkaPublisher {

    private static final String TOPIC = "created-user-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(UserCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.getUserId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Could not publish user-created event after commit: userId={}",
                                event.getUserId(), throwable);
                        return;
                    }
                    log.info("User-created event published after commit: userId={}", event.getUserId());
                });
    }
}
