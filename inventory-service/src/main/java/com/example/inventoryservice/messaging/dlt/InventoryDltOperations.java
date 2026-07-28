package com.example.inventoryservice.messaging.dlt;

import com.example.event.PaymentCancelledEvent;
import com.example.event.PaymentFailedEvent;
import com.example.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryDltOperations {

    private static final List<String> DLT_TOPICS = List.of(
            "payment-success.DLT",
            "payment-failed.DLT",
            "payment-cancelled.DLT"
    );

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public List<DltTopicStatus> status() {
        return DLT_TOPICS.stream()
                .map(topic -> new DltTopicStatus(topic, countRecords(topic)))
                .toList();
    }

    public void replayPaymentSuccess(PaymentSuccessEvent event) {
        send("payment-success", event.getOrderId(), event);
    }

    public void replayPaymentFailed(PaymentFailedEvent event) {
        send("payment-failed", event.getOrderId(), event);
    }

    public void replayPaymentCancelled(PaymentCancelledEvent event) {
        send("payment-cancelled", event.getOrderId(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event).get(5, TimeUnit.SECONDS);
            log.warn("DLT replay sent: sourceTopic={}, key={}, eventType={}", topic, key,
                    event.getClass().getSimpleName());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while replaying DLT message", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Could not replay DLT message to " + topic, exception);
        }
    }

    private long countRecords(String topic) {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            Set<String> topics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
            if (!topics.contains(topic)) {
                return 0;
            }

            TopicDescription description = adminClient.describeTopics(List.of(topic))
                    .allTopicNames().get(5, TimeUnit.SECONDS).get(topic);
            Map<TopicPartition, org.apache.kafka.clients.admin.OffsetSpec> earliest = new HashMap<>();
            Map<TopicPartition, org.apache.kafka.clients.admin.OffsetSpec> latest = new HashMap<>();
            description.partitions().forEach(partition -> {
                TopicPartition topicPartition = new TopicPartition(topic, partition.partition());
                earliest.put(topicPartition, org.apache.kafka.clients.admin.OffsetSpec.earliest());
                latest.put(topicPartition, org.apache.kafka.clients.admin.OffsetSpec.latest());
            });

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> firstOffsets =
                    adminClient.listOffsets(earliest).all().get(5, TimeUnit.SECONDS);
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> lastOffsets =
                    adminClient.listOffsets(latest).all().get(5, TimeUnit.SECONDS);
            return latest.keySet().stream()
                    .mapToLong(partition -> lastOffsets.get(partition).offset() - firstOffsets.get(partition).offset())
                    .sum();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading DLT status", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Could not read DLT status from Kafka", exception);
        }
    }
}
