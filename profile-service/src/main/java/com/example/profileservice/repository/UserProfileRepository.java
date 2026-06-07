package com.example.profileservice.repository;

import com.example.profileservice.entity.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    boolean existsByUserId(String userId);
    Optional<UserProfile> findByUserId(String userId);
}
