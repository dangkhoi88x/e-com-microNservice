package com.example.reviewservice.client;

import com.example.reviewservice.dto.response.ApiResponse;
import com.example.reviewservice.dto.response.ProfileSnapshotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PROFILE-CLIENT")
public class ProfileClient {
    private static final String DEFAULT_REVIEWER_NAME = "Khách hàng NovaShop";

    private final WebClient.Builder webClientBuilder;

    public String getPublicReviewerName(String authorization) {
        try {
            ApiResponse<ProfileSnapshotResponse> response = webClientBuilder.build()
                    .get()
                    .uri("http://PROFILE-SERVICE/api/v1/user-profile/me")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<ProfileSnapshotResponse>>() {})
                    .block();

            return response == null ? DEFAULT_REVIEWER_NAME : anonymize(response.getData());
        } catch (RuntimeException exception) {
            log.warn("Could not load profile snapshot for review; using safe default name");
            return DEFAULT_REVIEWER_NAME;
        }
    }

    private String anonymize(ProfileSnapshotResponse profile) {
        if (profile == null) {
            return DEFAULT_REVIEWER_NAME;
        }

        String firstName = normalize(profile.firstName());
        String lastName = normalize(profile.lastName());
        if (firstName.isEmpty() && lastName.isEmpty()) {
            return DEFAULT_REVIEWER_NAME;
        }
        if (lastName.isEmpty()) {
            return firstName;
        }
        if (firstName.isEmpty()) {
            return lastName.substring(0, 1).toUpperCase() + ".";
        }
        return firstName + " " + lastName.substring(0, 1).toUpperCase() + ".";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
