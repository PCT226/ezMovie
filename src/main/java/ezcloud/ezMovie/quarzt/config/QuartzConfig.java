package ezcloud.ezMovie.quarzt.config;

import ezcloud.ezMovie.quarzt.job.ShowtimeJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.CronScheduleBuilder;
import org.springframework.beans.factory.annotation.Autowired;
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
        // Thiết lập cron expression để chạy job vào lúc 23:59 mỗi ngày
        //String cronExpression = "0 59 23 * * ?";
        String cronExpression = "0 45 10 * * ?";

        return TriggerBuilder.newTrigger()
                .forJob(showtimeJobDetail())
                .withIdentity("showtimeTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
}
