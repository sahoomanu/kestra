package com.sapphireims.engine.service;

import com.sapphireims.engine.domain.WfTask;
import com.sapphireims.engine.quartz.DueDateJob;
import com.sapphireims.engine.quartz.FollowUpJob;
import java.time.ZoneId;
import org.quartz.JobBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.Scheduler;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.stereotype.Component;

@Component
public class TaskScheduler {
    private final Scheduler scheduler;

    public TaskScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void schedule(WfTask task) {
        try {
            if (task.getDueDate() != null) {
                JobDetail job = JobBuilder.newJob(DueDateJob.class)
                    .withIdentity("due-" + task.getId())
                    .usingJobData("taskId", task.getId())
                    .build();
                Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(java.util.Date.from(task.getDueDate().atZone(ZoneId.systemDefault()).toInstant()))
                    .build();
                scheduler.scheduleJob(job, trigger);
            }
            if (task.getFollowUpDate() != null) {
                JobDetail job = JobBuilder.newJob(FollowUpJob.class)
                    .withIdentity("follow-" + task.getId())
                    .usingJobData("taskId", task.getId())
                    .build();
                Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(java.util.Date.from(task.getFollowUpDate().atZone(ZoneId.systemDefault()).toInstant()))
                    .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
