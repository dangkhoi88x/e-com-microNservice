package com.example.microserviceecom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class MicroserviceEcomApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceEcomApplication.class, args);
    }

}
