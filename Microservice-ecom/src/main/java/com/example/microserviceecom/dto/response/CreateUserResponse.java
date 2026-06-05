package com.example.microserviceecom.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateUserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
}
