package com.example.microserviceecom.messaing.consumer;

import com.example.microserviceecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-PROFILE-COMPENSATION-CONSUMER")
public class UserProfileConsumer {
    private final UserRepository userRepository;


    @RetryableTopic(attempts = "4", backOff = @BackOff(value = 1000, multiplier = 2))
    @KafkaListener(topics = "user-profile-created-fail", groupId = "user-group")
    public void deleteErrorUser(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            userRepository.deleteById(userId);
            log.info("Deleted user because profile creation failed: userId={}", userId);
        });
    }
}
