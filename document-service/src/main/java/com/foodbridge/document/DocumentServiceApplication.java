package com.foodbridge.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for document-service: generates PDF donation receipts /
 * impact certificates and stores them in S3, driven by an SQS queue
 * (with DLQ) fed from {@code pickup.completed} Kafka events. The SQS poll
 * loop runs as a Quartz job (see the {@code quartz} package) backed by a
 * dedicated MySQL JobStore, rather than a Spring {@code @Scheduled} method.
 */
@SpringBootApplication
@EnableFeignClients
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
