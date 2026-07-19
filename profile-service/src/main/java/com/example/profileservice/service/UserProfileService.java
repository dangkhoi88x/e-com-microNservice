package com.example.profileservice.service;

import com.example.event.UserCreatedEvent;
import com.example.profileservice.dto.req.UpdateMyProfileRequest;
import com.example.profileservice.dto.res.UserProfileResponse;
import com.example.profileservice.entity.UserProfile;
import com.example.profileservice.exception.ErrorCode;
import com.example.profileservice.exception.ProfileServiceException;
import com.example.profileservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-PROFILE")
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public boolean createFromEvent(UserCreatedEvent event) {
        if (userProfileRepository.existsByUserId(event.getUserId())) {
            log.info("User profile already exists for userId={}", event.getUserId());
            return false;
        }

        UserProfile userProfile = UserProfile.builder()
                .userId(event.getUserId())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .build();

        userProfileRepository.save(userProfile);

        log.info("Created user profile successfully: userId={}", event.getUserId());
        return true;
    }

    public UserProfileResponse myInfo(String userId) {

        UserProfile userProfile= userProfileRepository.findByUserId(userId)
                .orElseThrow(()-> new ProfileServiceException(ErrorCode.USER_NOT_FOUND));
         return  UserProfileResponse.builder()
                 .userId(userProfile.getUserId())
            .firstName(userProfile.getFirstName())
            .lastName(userProfile.getLastName())
            .avatarUrl(userProfile.getAvatarUrl())
            .bio(userProfile.getBio())
            .birthDate(userProfile.getBirthDate())
            .phoneNumber(userProfile.getPhoneNumber())
            .address(userProfile.getAddress())
            .city(userProfile.getCity())
            .postalCode(userProfile.getPostalCode())
            .build();
        }
    public UserProfileResponse updateMyProfile(String userId, UpdateMyProfileRequest request) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(()-> new ProfileServiceException(ErrorCode.USER_NOT_FOUND));
        userProfile.setFirstName(request.firstName());
        userProfile.setLastName(request.lastName());
        userProfile.setAvatarUrl(request.avatarUrl());
        userProfile.setBio(request.bio());
        userProfile.setBirthDate(request.birthDate());
        userProfile.setPhoneNumber(request.phoneNumber());
        userProfile.setAddress(request.address());
        userProfile.setCity(request.city());
        userProfile.setPostalCode(request.postalCode());
        UserProfile savedProfile = userProfileRepository.save(userProfile);

        return UserProfileResponse.builder()
                .userId(savedProfile.getUserId())
                .firstName(savedProfile.getFirstName())
                .lastName(savedProfile.getLastName())
                .avatarUrl(savedProfile.getAvatarUrl())
                .bio(savedProfile.getBio())
                .birthDate(savedProfile.getBirthDate())
                .phoneNumber(savedProfile.getPhoneNumber())
                .address(savedProfile.getAddress())
                .city(savedProfile.getCity())
                .postalCode(savedProfile.getPostalCode())
                .build();
    }
}
