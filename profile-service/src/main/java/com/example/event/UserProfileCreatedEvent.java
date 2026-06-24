package com.example.event;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserProfileCreatedEvent {
    private String email;
    private String firstName;
    private String lastName;
}
