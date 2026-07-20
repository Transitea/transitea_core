package com.transitea.config;

import com.transitea.job.RecapitulatifQuotidienJob;
import com.transitea.job.ReinitialisationQuotaMensuelJob;
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

    @Bean
    public JobDetail reinitialisationQuotaMensuelJobDetail() {
        return JobBuilder.newJob(ReinitialisationQuotaMensuelJob.class)
                .withIdentity("reinitialisationQuotaMensuel")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reinitialisationQuotaMensuelTrigger(JobDetail reinitialisationQuotaMensuelJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reinitialisationQuotaMensuelJobDetail)
                .withIdentity("reinitialisationQuotaMensuelTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 1 * ?"))
                .build();
    }
}
