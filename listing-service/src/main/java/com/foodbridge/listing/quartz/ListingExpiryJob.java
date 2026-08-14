package com.foodbridge.listing.quartz;

import com.foodbridge.listing.service.ListingService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz job that expires stale listings and raises expiry warnings.
 * Replaces the previous {@code @Scheduled}-based poller: Quartz's JDBC
 * JobStore (see {@link QuartzDataSourceConfig}) means the trigger's fire
 * state survives a restart and, if this service is ever scaled to multiple
 * instances, only one instance fires the job per trigger interval instead
 * of every instance independently polling.
 *
 * <p>Extends {@link QuartzJobBean} rather than implementing {@link Job}
 * directly so Spring injects {@code listingService} via the autowiring
 * job factory Spring Boot configures automatically for Quartz.</p>
 */
@Slf4j
public class ListingExpiryJob extends QuartzJobBean {

    @Autowired
    private transient ListingService listingService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            listingService.expireStaleListings();
        } catch (Exception ex) {
            // A job failure must never propagate uncaught into Quartz's misfire handling in a
            // way that leaves the trigger in an unexpected state; log loudly and let the next
            // scheduled fire retry naturally.
            log.error("ListingExpiryJob execution failed", ex);
            throw new JobExecutionException(ex, false);
        }
    }
}
