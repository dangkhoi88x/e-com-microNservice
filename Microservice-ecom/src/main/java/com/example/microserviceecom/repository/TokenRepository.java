package com.example.microserviceecom.repository;

import com.example.microserviceecom.entity.Token;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
//revoked JWT trong Redis
@Repository
public interface TokenRepository extends CrudRepository<Token, String> {
}
