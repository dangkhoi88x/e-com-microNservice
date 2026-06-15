package com.example.profileservice.messaing.consumer;


import com.example.event.UserCreatedEvent;
import com.example.profileservice.entity.UserProfile;
import com.example.profileservice.repository.UserProfileRepository;
import com.example.profileservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-PROFILE-CONSUMER")
public class UserProfileConsumer {
//    private final UserProfileRepository UserProfileRepository;
//
//    @KafkaListener(topics = "created-user-topic",groupId = "user-profile-group")
//    public void userCreatedEvent(UserCreatedEvent event) {
//            UserProfile userProfile = UserProfile.builder()
//                    .userId(event.getUserId())
//                    .firstName(event.getFirstName())
//                    .lastName(event.getLastName())
//                    .build();
//        UserProfileRepository.save(userProfile);
//        log.info("UserProfile Created Successfully",event.getUserId());
//    }
private final UserProfileService userProfileService;

    @KafkaListener(topics = "created-user-topic", groupId = "user-profile-group")
    public void userCreatedEvent(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent: userId={}, firstName={}, lastName={}",
                event.getUserId(),
                event.getFirstName(),
                event.getLastName());

        userProfileService.createFromEvent(event);
    }



}
