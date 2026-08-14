package com.foodbridge.claim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for claim-service — the platform's core consistency-critical
 * path. Guarantees exactly one NGO can successfully claim a given listing
 * using a Redisson distributed lock backed by a MySQL unique-constraint
 * safety net (see {@code ClaimServiceImpl} and {@code V1__create_claims_table.sql}).
 */
@SpringBootApplication
@EnableFeignClients
public class ClaimServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimServiceApplication.class, args);
    }
}
