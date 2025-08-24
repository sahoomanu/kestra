package com.sapphireims.engine.quartz;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {
    @Bean
    public Scheduler scheduler() throws Exception {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.afterPropertiesSet();
        return factory.getScheduler();
    }
}
