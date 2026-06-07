package com.example.profileservice.service;

import com.example.profileservice.dto.req.CreateUserProfileRequest;
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
    public void create(CreateUserProfileRequest request) {
        if(userProfileRepository.existsByUserId(request.userId())) {
           throw new ProfileServiceException(ErrorCode.USER_PROFILE_EXISTED);
        }
        UserProfile userProfile = UserProfile.builder()
                .userId(request.userId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();

        userProfileRepository.save(userProfile);
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
            .build();
        }

}
