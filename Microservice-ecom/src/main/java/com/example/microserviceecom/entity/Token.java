package com.example.microserviceecom.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@RedisHash("token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//redis entity
public class Token {

    @Id
    private String tokenId; //JWT jti

    private String userId;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private long timeToLive;
}
