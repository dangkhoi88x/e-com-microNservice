package com.example.mediaservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "owner_id")
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaPurpose purpose;

    @Column(nullable = false, unique = true, name = "object_key")
    private String objectKey;

    @Column(nullable = false, name = "content_type", length = 100)
    private String contentType;

    @Column(nullable = false, name = "size_bytes")
    private long sizeBytes;

    @Column(nullable = false, name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
