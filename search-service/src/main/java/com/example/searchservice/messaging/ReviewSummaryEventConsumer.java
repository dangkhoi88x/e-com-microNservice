package com.example.searchservice.messaging;

import com.example.searchservice.service.ProductDocumentService;
import event.ReviewSummaryChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "REVIEW-SUMMARY-CONSUMER")
public class ReviewSummaryEventConsumer {
    private final ProductDocumentService productDocumentService;

    @KafkaListener(topics = "review-summary-changed", groupId = "search-group")
    public void consume(ReviewSummaryChangedEvent event) {
        productDocumentService.updateReviewSummary(
                event.productId(),
                event.averageRating(),
                event.reviewCount()
        );
    }
}
