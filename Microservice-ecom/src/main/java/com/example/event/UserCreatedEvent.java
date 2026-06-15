package com.example.event;

import lombok.*;
import org.springframework.stereotype.Service;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserCreatedEvent {
    private String userId;
    private String firstName;
    private String lastName;
}
