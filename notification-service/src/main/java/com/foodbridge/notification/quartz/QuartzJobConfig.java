package com.foodbridge.notification.quartz;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

@Configuration
public class QuartzJobConfig {

    @Bean
    public JobDetail notificationJobPollerJobDetail() {
        return JobBuilder.newJob(NotificationJobPollerJob.class)
                .withIdentity("notificationJobPollerJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger notificationJobPollerTrigger(JobDetail notificationJobPollerJobDetail,
                                                 @Value("${notification.job-poller.fixed-delay-ms:5000}") long fixedDelayMs) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(notificationJobPollerJobDetail);
        factoryBean.setStartDelay(5000L);
        factoryBean.setRepeatInterval(fixedDelayMs);
        factoryBean.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        factoryBean.setName("notificationJobPollerTrigger");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
