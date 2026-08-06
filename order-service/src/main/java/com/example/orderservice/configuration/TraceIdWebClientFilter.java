package com.example.orderservice.configuration;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

public final class TraceIdWebClientFilter {

    private TraceIdWebClientFilter() {
    }

    public static ExchangeFilterFunction propagateTraceId() {
        return (request, next) -> {
            String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
            if (!StringUtils.hasText(traceId)) {
                return next.exchange(request);
            }

            ClientRequest requestWithTraceId = ClientRequest.from(request)
                    .headers(headers -> headers.set(TraceIdFilter.TRACE_ID_HEADER, traceId))
                    .build();
            return next.exchange(requestWithTraceId);
        };
    }
}
