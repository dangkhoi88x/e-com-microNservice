package com.example.notificationservice.configuration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class KafkaTraceIdRecordInterceptor implements RecordInterceptor<String, Object> {

    @Override
    public ConsumerRecord<String, Object> intercept(
            ConsumerRecord<String, Object> record,
            Consumer<String, Object> consumer
    ) {
        Header traceIdHeader = record.headers().lastHeader(TraceIdFilter.TRACE_ID_HEADER);
        String traceId = traceIdHeader == null
                ? null
                : new String(traceIdHeader.value(), StandardCharsets.UTF_8);

        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, traceId);
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<String, Object> record, Consumer<String, Object> consumer) {
        MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
    }
}
