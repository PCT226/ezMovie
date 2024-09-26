package ezcloud.ezMovie.quarzt.config;

import ezcloud.ezMovie.quarzt.job.JobA;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail jobADetail() {
        return JobBuilder.newJob(JobA.class)
                .withIdentity("jobA")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger jobATrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(jobADetail())
                .withIdentity("triggerA")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(10)
                        .repeatForever())
                .build();
    }
}
