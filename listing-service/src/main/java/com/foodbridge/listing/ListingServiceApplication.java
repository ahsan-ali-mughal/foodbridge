package com.foodbridge.listing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for listing-service: owns the lifecycle of food donation
 * listings (create, geo-search, expire) and their image assets in S3.
 * Listing expiry runs as a Quartz job (see the {@code quartz} package)
 * rather than a Spring {@code @Scheduled} method, backed by a dedicated
 * MySQL-based JobStore.
 */
@SpringBootApplication
@EnableFeignClients
public class ListingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListingServiceApplication.class, args);
    }
}
