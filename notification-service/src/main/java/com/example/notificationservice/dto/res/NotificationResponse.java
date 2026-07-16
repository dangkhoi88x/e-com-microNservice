package com.example.notificationservice.dto.res;

import com.example.notificationservice.common.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter

public class NotificationResponse {
    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private LocalDateTime createdAt;
}
