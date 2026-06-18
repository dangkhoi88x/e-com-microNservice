package com.example.notificationservice.service;

import com.example.event.UserCreatedEvent;
import com.example.notificationservice.common.NotificationType;
import com.example.notificationservice.dto.res.NotificationResponse;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.mapper.NotificationMapper;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j(topic = "NOTIFICATION-SERVICE")
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void createNotificationWelcome(UserCreatedEvent event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .title("Welcome")
                .message("Welcome " + event.getFirstName() + " " + event.getLastName())
                .type(NotificationType.USER_CREATED)
                .build();

        notificationRepository.save(notification);
        log.info("Created welcome notification for userId={}", event.getUserId());

    }
    public List<NotificationResponse> myNotifications(String userId){
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }
}
