package com.example.shippingservice.messaging.dlt;

/**
 * Number of records currently retained by one dead-letter topic.
 */
public record DltTopicStatus(String topic, long messageCount) {
}
