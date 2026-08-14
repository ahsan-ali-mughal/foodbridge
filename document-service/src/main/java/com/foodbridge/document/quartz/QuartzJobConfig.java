package com.foodbridge.document.quartz;

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
    public JobDetail receiptGenerationJobDetail() {
        return JobBuilder.newJob(ReceiptGenerationJob.class)
                .withIdentity("receiptGenerationJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger receiptGenerationJobTrigger(JobDetail receiptGenerationJobDetail,
                                                @Value("${document.job-poller.fixed-delay-ms:5000}") long fixedDelayMs) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(receiptGenerationJobDetail);
        factoryBean.setStartDelay(5000L);
        factoryBean.setRepeatInterval(fixedDelayMs);
        factoryBean.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        factoryBean.setName("receiptGenerationJobTrigger");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
