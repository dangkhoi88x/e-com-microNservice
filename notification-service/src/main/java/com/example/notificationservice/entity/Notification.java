package com.example.notificationservice.entity;


import com.example.notificationservice.common.NotificationType;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @MongoId
    private String id;

    private String userId;

    private String title;

    private String message;

    private NotificationType type;

    @Builder.Default
    private boolean read = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}