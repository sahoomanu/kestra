package com.sapphireims.engine.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DueDateJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(DueDateJob.class);
    @Override
    public void execute(JobExecutionContext context) {
        Long taskId = context.getMergedJobDataMap().getLong("taskId");
        log.info("Task {} is due", taskId);
    }
}
