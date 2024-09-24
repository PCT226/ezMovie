package ezcloud.ezMovie.quarzt.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(JobFactory jobFactory) {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setJobFactory(jobFactory);
        schedulerFactory.setAutoStartup(true);
        return schedulerFactory;
    }
//@Bean
//public SchedulerFactoryBean schedulerFactoryBean() {
//    SchedulerFactoryBean factory = new SchedulerFactoryBean();
//    factory.setJobFactory(jobFactory());
//    return factory;
//}

    @Bean
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean)throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        scheduler.start(); // Khởi động scheduler
        return scheduler;
    }

    @Bean
    public JobFactory jobFactory(AutowiringSpringBeanJobFactory autowiringSpringBeanJobFactory) {
        return autowiringSpringBeanJobFactory; // Đăng ký JobFactory tùy chỉnh
    }
}
