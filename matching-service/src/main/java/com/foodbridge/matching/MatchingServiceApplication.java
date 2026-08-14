package com.foodbridge.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for matching-service: maintains NGO location profiles and,
 * on every {@code donation.created} event, computes the set of nearby
 * eligible NGOs and publishes {@code donation.matched}.
 */
@SpringBootApplication
public class MatchingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchingServiceApplication.class, args);
    }
}
