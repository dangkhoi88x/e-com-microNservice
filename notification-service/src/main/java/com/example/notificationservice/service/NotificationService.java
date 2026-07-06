package com.example.notificationservice.service;

import com.example.event.OrderCreatedEvent;
import com.example.event.UserProfileCreatedEvent;
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

    public void createNotificationWelcome(UserProfileCreatedEvent event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .title("Welcome")
                .message("Welcome " + event.getFirstName() + " " + event.getLastName()+ " " + event.getEmail() )
                .type(NotificationType.USER_CREATED)
                .build();

        notificationRepository.save(notification);
        log.info("Created welcome notification for userId={}", event.getUserId());

    }

    public void createNotificationOrderCreated(OrderCreatedEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .title("Order created")
                .message("Your order " + event.getOrderId() + " has been created successfully. Total: " + event.getTotalAmount())
                .type(NotificationType.ORDER_CREATED)
                .build();

        notificationRepository.save(notification);
        log.info("Created order notification for userId={}, orderId={}", event.getUserId(), event.getOrderId());
    }

    public List<NotificationResponse> myNotifications(String userId){
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }
}
