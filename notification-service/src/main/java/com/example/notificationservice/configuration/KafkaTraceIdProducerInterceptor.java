package com.example.notificationservice.configuration;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KafkaTraceIdProducerInterceptor implements ProducerInterceptor<String, Object> {

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (StringUtils.hasText(traceId)) {
            Headers headers = record.headers();
            headers.remove(TraceIdFilter.TRACE_ID_HEADER);
            headers.add(TraceIdFilter.TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
