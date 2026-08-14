package com.foodbridge.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for notification-service: fans out time-critical alerts via
 * SNS (SMS + email + push) and processes non-urgent notification jobs from
 * an SQS queue with a dead-letter queue for failures. The SQS poll loop
 * runs as a Quartz job (see the {@code quartz} package) backed by a
 * dedicated MySQL JobStore, rather than a Spring {@code @Scheduled} method.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
