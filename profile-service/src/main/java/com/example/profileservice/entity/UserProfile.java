package com.example.profileservice.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;

@Document(collection = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @MongoId
    private String id;
    @NotBlank
    @Indexed(unique = true)
    private String userId;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String avatarUrl;
    private String bio;
    private LocalDate birthDate;

}
