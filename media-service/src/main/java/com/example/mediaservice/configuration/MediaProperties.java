package com.example.mediaservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(String publicBaseUrl) {
}
