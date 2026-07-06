package com.example.notificationservice.service;

import com.example.event.OrderCancelledEvent;
import com.example.event.OrderCreatedEvent;
import com.example.event.OrderStatusUpdatedEvent;
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

    public void createNotificationOrderCancelled(OrderCancelledEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .title("Order cancelled")
                .message("Your order " + event.getOrderId() + " has been cancelled successfully.")
                .type(NotificationType.ORDER_CANCELLED)
                .build();

        notificationRepository.save(notification);
        log.info("Created order cancelled notification for userId={}, orderId={}", event.getUserId(), event.getOrderId());
    }

    public void createNotificationOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .title("Order status updated")
                .message("Your order " + event.getOrderId() + " status has been updated to " + event.getNewStatus())
                .type(NotificationType.ORDER_STATUS_UPDATED)
                .build();

        notificationRepository.save(notification);
        log.info("Created order status updated notification for userId={}, orderId={}, newStatus={}",
                event.getUserId(),
                event.getOrderId(),
                event.getNewStatus());
    }

    public List<NotificationResponse> myNotifications(String userId){
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }
}
