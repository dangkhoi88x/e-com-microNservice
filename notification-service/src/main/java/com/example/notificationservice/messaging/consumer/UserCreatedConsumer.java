package com.example.notificationservice.messaging.consumer;

import com.example.event.UserProfileCreatedEvent;
import com.example.notificationservice.service.MailService;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-CREATED-NOTIFICATION-CONSUMER")

public class UserCreatedConsumer {
    private final MailService mailService;
    private final NotificationService notificationService;

    @KafkaListener(topics = "created-profile-created", groupId = "notification-group")
    public void userProfileCreated(UserProfileCreatedEvent event) {
        if (!StringUtils.hasText(event.getEmail())) {
            log.warn("Skip welcome email because profile-created event has no email: firstName={}, lastName={}",
                    event.getFirstName(),
                    event.getLastName());
            return;
        }

        notificationService.createNotificationWelcome(event);

        String fullName = event.getFirstName() + " " + event.getLastName();
        mailService.sendEmailWithTemplate(
                event.getEmail(),
                fullName,
                "Chao mung ban den voi Thue Xe Tu Lai",
                "welcome-gmail"
        );
    }
}
