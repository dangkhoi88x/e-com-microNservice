package com.example.profileservice.controller;

import com.example.event.UserCreatedEvent;
import com.example.profileservice.messaging.dlt.DltTopicStatus;
import com.example.profileservice.messaging.dlt.ProfileDltOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Demo-only operational endpoints. Copy a verified payload from Kafka UI before replaying it.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-profile/admin/dlt")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ProfileDltAdminController {

    private final ProfileDltOperations dltOperations;

    @GetMapping
    public List<DltTopicStatus> status() {
        return dltOperations.status();
    }

    @PostMapping("/created-user/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void replayUserCreated(@RequestBody UserCreatedEvent event) {
        dltOperations.replayUserCreated(event);
    }
}
