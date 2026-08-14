package com.foodbridge.logistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for logistics-service: creates a pickup task whenever a
 * donation is claimed, and tracks it through to delivery completion.
 */
@SpringBootApplication
public class LogisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsServiceApplication.class, args);
    }
}
