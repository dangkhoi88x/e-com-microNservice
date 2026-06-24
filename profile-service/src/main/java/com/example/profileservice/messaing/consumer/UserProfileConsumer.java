package com.example.profileservice.messaing.consumer;


import com.example.event.UserCreatedEvent;
import com.example.event.UserProfileCreatedEvent;
import com.example.profileservice.entity.UserProfile;
import com.example.profileservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-PROFILE-CONSUMER")
public class UserProfileConsumer {
    private final UserProfileRepository userProfileRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @KafkaListener(topics = "created-user-topic",groupId = "user-profile-group")
    public void userCreatedEvent(UserCreatedEvent event) {
        try {
            UserProfile userProfile = UserProfile.builder()
                    .userId(event.getUserId())
                    .firstName(event.getFirstName())
                    .lastName(event.getLastName())
                    .build();
            userProfileRepository.save(userProfile);
            UserProfileCreatedEvent profileCreatedEvent = UserProfileCreatedEvent.builder()
                    .email(event.getEmail())
                    .firstName(event.getFirstName())
                    .lastName(event.getLastName())
                    .build();
            log.info("UserProfile Created Successfully: userId={}", event.getUserId());

            kafkaTemplate.send("created-profile-created", profileCreatedEvent);
        } catch (Exception e) {
            log.error("UserProfile Creation Failed {}", event.getUserId(), e);
            kafkaTemplate.send("user-profile-created-fail", event.getUserId());
        }

    }
//private final UserProfileService userProfileService;
//
//    @KafkaListener(topics = "created-user-topic", groupId = "user-profile-group")
//    @RetryableTopic(attempts = "4", backOff = @BackOff(value = 1000, multiplier = 2))
//    public void userCreatedEvent(UserCreatedEvent event) {
//        try{
//            log.info("Received UserCreatedEvent: userId={}, firstName={}, lastName={}",
//                    event.getUserId(),
//                    event.getFirstName(),
//                    event.getLastName());
//
//            userProfileService.createFromEvent(event);
//        }
//
//
//    }



}
