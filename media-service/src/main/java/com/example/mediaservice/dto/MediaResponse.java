package com.example.mediaservice.dto;

import com.example.mediaservice.entity.MediaPurpose;

import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
        UUID id,
        String objectKey,
        MediaPurpose purpose,
        String contentType,
        long sizeBytes,
        Instant createdAt,
        String contentUrl,
        String downloadUrl
) {
}
