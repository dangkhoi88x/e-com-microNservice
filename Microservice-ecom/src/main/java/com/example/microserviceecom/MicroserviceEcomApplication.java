package com.example.microserviceecom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MicroserviceEcomApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceEcomApplication.class, args);
    }

}
