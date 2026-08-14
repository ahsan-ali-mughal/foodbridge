package com.foodbridge.notification.service;

public interface SnsPublisherService {

    /**
     * Publishes to the platform's urgent-alerts SNS topic, which fans out to
     * every subscribed channel (SMS, email, push) independently.
     *
     * @return the SNS message id, for correlation with delivery logs
     */
    String publishUrgentAlert(String subject, String message);
}
