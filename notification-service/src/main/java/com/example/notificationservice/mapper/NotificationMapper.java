package com.example.notificationservice.mapper;

import com.example.notificationservice.dto.res.NotificationResponse;
import com.example.notificationservice.entity.Notification;

public class NotificationMapper {

    public static NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
