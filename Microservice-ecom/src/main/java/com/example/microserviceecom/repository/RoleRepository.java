package com.example.microserviceecom.repository;

import com.example.microserviceecom.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByNameIgnoreCase(String name);
}
