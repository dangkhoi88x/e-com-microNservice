package com.example.profileservice.messaing.consumer;


import com.example.event.UserCreatedEvent;
import com.example.profileservice.entity.UserProfile;
import com.example.profileservice.repository.UserProfileRepository;
import com.example.profileservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-PROFILE-CONSUMER")
public class UserProfileConsumer {
    private final UserProfileRepository UserProfileRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @KafkaListener(topics = "created-user-topic",groupId = "user-profile-group")
    public void userCreatedEvent(UserCreatedEvent event) {
        try{    UserProfile userProfile = UserProfile.builder()
                .userId(event.getUserId())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .build();
            UserProfileRepository.save(userProfile);
            log.info("UserProfile Created Successfully",event.getUserId());}
        catch (Exception e){
            log.error("UserProfile Creation Failed {}",event.getUserId(),e);
            kafkaTemplate.send("user-profile-created-fail",event.getUserId());
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
