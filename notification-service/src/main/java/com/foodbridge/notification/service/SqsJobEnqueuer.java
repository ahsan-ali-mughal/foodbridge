package com.foodbridge.notification.service;

public interface SqsJobEnqueuer {

    /**
     * Enqueues a non-urgent notification job (e.g. welcome email, claim
     * confirmation) onto the notification job queue for asynchronous
     * processing by {@code NotificationJobPoller}.
     */
    void enqueue(String jobType, String recipientId, String payloadJson);
}
