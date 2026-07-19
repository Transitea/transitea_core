package com.transitea.config;

import com.transitea.job.RecapitulatifQuotidienJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurationQuartz {

    @Bean
    public JobDetail recapitulatifQuotidienJobDetail() {
        return JobBuilder.newJob(RecapitulatifQuotidienJob.class)
                .withIdentity("recapitulatifQuotidien")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger recapitulatifQuotidienTrigger(JobDetail recapitulatifQuotidienJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(recapitulatifQuotidienJobDetail)
                .withIdentity("recapitulatifQuotidienTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(18, 0))
                .build();
    }
}
