package com.example.orderservice.configuration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
@Configuration
public class WebClientConfiguration {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(TraceIdWebClientFilter.propagateTraceId());
    }

    @Bean
    public WebClient productWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://PRODUCT-SERVICE")
                .build();
    }
}
