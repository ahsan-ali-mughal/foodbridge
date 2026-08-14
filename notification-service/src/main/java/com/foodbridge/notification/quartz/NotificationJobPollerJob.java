package com.foodbridge.notification.quartz;

import com.foodbridge.notification.service.impl.NotificationJobPoller;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz job wrapping {@link NotificationJobPoller#poll()}. Replaces the
 * previous {@code @Scheduled(fixedDelay=...)} method: with Quartz's JDBC
 * JobStore, exactly one running instance of notification-service polls
 * SQS on each trigger fire even if the service is scaled horizontally,
 * instead of every instance polling independently and competing for the
 * same messages.
 */
@Slf4j
public class NotificationJobPollerJob extends QuartzJobBean {

    @Autowired
    private transient NotificationJobPoller notificationJobPoller;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            notificationJobPoller.poll();
        } catch (Exception ex) {
            log.error("NotificationJobPollerJob execution failed", ex);
            throw new JobExecutionException(ex, false);
        }
    }
}
