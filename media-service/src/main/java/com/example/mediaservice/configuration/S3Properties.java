package com.example.mediaservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        long maxFileSizeBytes,
        Duration downloadUrlTtl
) {
}
