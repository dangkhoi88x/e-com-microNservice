package com.example.paymentservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        String successUrl,
        String cancelUrl,
        String currency
) {
}
