package com.foodbridge.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for admin-service: a server-rendered (Thymeleaf) operations
 * dashboard for platform administrators — NGO verification approvals,
 * live activity feed, and summary analytics.
 */
@SpringBootApplication
@EnableFeignClients
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
