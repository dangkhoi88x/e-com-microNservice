package com.example.searchservice.messaging;

import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.service.ProductDocumentService;
import event.ProductCreatedEvent;
import event.ProductDeletedEvent;
import event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PRODUCT-EVENT-CONSUMER")
public class ProductEventConsumer {
    private final ProductDocumentService productDocumentService;

    @KafkaListener(topics = "product-created", groupId = "search-group")
    public void productEvent(ProductCreatedEvent event) {
        log.info("Consumed ProductCreatedEvent: {}", event.getProductId());
        ProductDocument productDocument = convertToDocument(event);
        productDocumentService.saveProductDocument(productDocument);
        log.info("Indexed product successfully: {}", productDocument.getProductId());

    }
    @KafkaListener(topics = "product-deleted", groupId = "search-group")
    public void productDeletedEvent(ProductDeletedEvent event) {
        log.info("Consumed ProductDeletedEvent: {}", event.getProductId());
        productDocumentService.deleteProductDocument(event.getProductId());
        log.info("Deleted product document successfully: {}", event.getProductId());
    }

    @KafkaListener(topics = "product-updated", groupId = "search-group")
    public void productUpdatedEvent(ProductUpdatedEvent event) {
        log.info("Consumed ProductUpdatedEvent: {}", event.getProductId());
        ProductDocument productDocument = convertToDocument(event);
        productDocumentService.saveProductDocument(productDocument);
        log.info("Updated product document successfully: {}", productDocument.getProductId());
    }

    private ProductDocument convertToDocument(ProductCreatedEvent event) {
        return ProductDocument.builder()
                .productId(event.getProductId())
                .name(event.getName())
                .description(event.getDescription())
                .price(event.getPrice())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .categoryId(event.getCategoryId())
                .categoryName(event.getCategoryName())
                .thumbnailUrl(event.getThumbnailUrl())
                .inStock(event.getInStock())
                .build();
    }

    private ProductDocument convertToDocument(ProductUpdatedEvent event) {
        return ProductDocument.builder()
                .productId(event.getProductId())
                .name(event.getName())
                .description(event.getDescription())
                .price(event.getPrice())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .categoryId(event.getCategoryId())
                .categoryName(event.getCategoryName())
                .thumbnailUrl(event.getThumbnailUrl())
                .inStock(event.getInStock())
                .build();
    }
}
