package com.foodbridge.listing.quartz;

import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

@Configuration
public class QuartzJobConfig {

    @Bean
    public JobDetail listingExpiryJobDetail() {
        return org.quartz.JobBuilder.newJob(ListingExpiryJob.class)
                .withIdentity("listingExpiryJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger listingExpiryJobTrigger(JobDetail listingExpiryJobDetail,
                                            @Value("${listing.expiry-scheduler.fixed-delay-ms:60000}") long fixedDelayMs) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(listingExpiryJobDetail);
        factoryBean.setStartDelay(5000L); // small initial delay so the app is fully up before first fire
        factoryBean.setRepeatInterval(fixedDelayMs);
        factoryBean.setRepeatCount(org.quartz.SimpleTrigger.REPEAT_INDEFINITELY);
        factoryBean.setMisfireInstruction(SimpleScheduleBuilder.simpleSchedule().build().getMisfireInstruction());
        factoryBean.setName("listingExpiryJobTrigger");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
