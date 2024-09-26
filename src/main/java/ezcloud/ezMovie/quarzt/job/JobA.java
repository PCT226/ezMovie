package ezcloud.ezMovie.quarzt.job;

import ezcloud.ezMovie.booking.service.TicketService;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.service.ShowtimeService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
public class JobA implements Job {

    @Autowired
    private ShowtimeService showtimeService;
    @Autowired
    private TicketService ticketService;
    @Autowired
    private Scheduler scheduler;

    @Override
    public void execute(JobExecutionContext context) {
        // Thực hiện Job B
        if(!ticketService.isRedisAlive()) {
            List<Showtime> showtimes = showtimeService.getShowtimeNow();

            for (Showtime showtime:showtimes){
                int ShowtimeId=showtime.getId();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.of(showtime.getDate(),showtime.getEndTime());
                long ttlSeconds = Duration.between(now, endTime).getSeconds();
                scheduleJobB(ttlSeconds,ShowtimeId);
            }
            try {
                scheduler.pauseTrigger(TriggerKey.triggerKey("triggerA"));
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private void scheduleJobB(long ttlSeconds, Integer showtimeID) {
        try {
            String jobIdentity = "jobB_" + showtimeID ;
            String triggerIdentity = "triggerB_" + showtimeID ;

            JobDetail jobDetail = JobBuilder.newJob(JobB.class)
                    .withIdentity(jobIdentity)
                    .usingJobData("showtimeId", showtimeID)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerIdentity)
                    .startAt(Date.from(Instant.now().plusSeconds(3)))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

}

