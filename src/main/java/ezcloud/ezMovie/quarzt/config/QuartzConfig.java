package ezcloud.ezMovie.quarzt.config;

import ezcloud.ezMovie.quarzt.job.ShowtimeJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {


    @Bean
    public JobDetail showtimeJobDetail() {
        return JobBuilder.newJob(ShowtimeJob.class)
                .withIdentity("showtimeJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger showtimeJobTrigger() {
        String cronExpression = "0 59 23 * * ?";
        //String cronExpression = "0 45 10 * * ?";

        return TriggerBuilder.newTrigger()
                .forJob(showtimeJobDetail())
                .withIdentity("showtimeTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
}
