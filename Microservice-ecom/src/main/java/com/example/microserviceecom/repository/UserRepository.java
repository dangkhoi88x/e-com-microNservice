package com.example.microserviceecom.repository;

import com.example.microserviceecom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
}
