package com.example.profileservice.messaing.consumer;

import com.example.event.UserCreatedEvent;
import com.example.event.UserProfileCreatedEvent;
import com.example.profileservice.exception.ProfileServiceException;
import com.example.profileservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-PROFILE-CONSUMER")
public class UserProfileConsumer {

    private final UserProfileService userProfileService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(value = 1_000, multiplier = 2),
            retryTopicSuffix = "-retry",
            dltTopicSuffix = ".DLT",
            exclude = ProfileServiceException.class
    )
    @KafkaListener(topics = "created-user-topic", groupId = "user-profile-group")
    public void userCreatedEvent(UserCreatedEvent event) {
        boolean created = userProfileService.createFromEvent(event);
        if (!created) {
            return;
        }

        UserProfileCreatedEvent profileCreatedEvent = UserProfileCreatedEvent.builder()
                .userId(event.getUserId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .build();
        log.info("UserProfile created successfully: userId={}", event.getUserId());
        kafkaTemplate.send("created-profile-created", profileCreatedEvent);
    }

    @DltHandler
    public void userCreatedDlt(UserCreatedEvent event) {
        if (event == null || event.getUserId() == null || event.getUserId().isBlank()) {
            log.error("UserCreatedEvent reached DLT without a valid userId; compensation cannot be sent");
            return;
        }

        log.error("User profile creation exhausted retries; publishing compensation event: userId={}", event.getUserId());
        kafkaTemplate.send("user-profile-created-fail", event.getUserId());
    }
}
