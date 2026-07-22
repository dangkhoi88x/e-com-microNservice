package com.example.reviewservice.messaging;

import event.ReviewSummaryChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "REVIEW-SUMMARY-PUBLISHER")
public class ReviewSummaryEventPublisher {
    public static final String TOPIC = "review-summary-changed";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAfterCommit(ReviewSummaryChangedEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(TOPIC, event.productId(), event)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                log.error("Could not publish review summary: productId={}", event.productId(), error);
                            } else {
                                log.info("Published review summary: productId={}, rating={}, count={}",
                                        event.productId(), event.averageRating(), event.reviewCount());
                            }
                        });
            }
        });
    }
}
