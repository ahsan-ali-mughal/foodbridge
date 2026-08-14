package com.foodbridge.document.quartz;

import com.foodbridge.document.service.impl.ReceiptGenerationJobPoller;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
public class ReceiptGenerationJob extends QuartzJobBean {

    @Autowired
    private transient ReceiptGenerationJobPoller receiptGenerationJobPoller;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            receiptGenerationJobPoller.poll();
        } catch (Exception ex) {
            log.error("ReceiptGenerationJob execution failed", ex);
            throw new JobExecutionException(ex, false);
        }
    }
}
