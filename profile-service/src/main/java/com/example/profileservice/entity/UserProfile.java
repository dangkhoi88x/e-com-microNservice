package com.example.profileservice.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;

@Document(collection = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @MongoId
    private String id;
    @NotBlank
    private String userID;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String bio;
    private LocalDate birthDate;

}
