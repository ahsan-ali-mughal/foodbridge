package com.foodbridge.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for auth-service: owns user identity, credential storage,
 * JWT issuance, and role/verification-status management for the whole platform.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
