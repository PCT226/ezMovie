package ezcloud.ezMovie.quarzt.job;

import ezcloud.ezMovie.service.SeatService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UpdateSeatStatusJob implements Job {

    @Autowired
    private SeatService seatService; // Dịch vụ để cập nhật trạng thái ghế

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Lấy dữ liệu từ JobDataMap (showtimeId truyền từ Scheduler)
        String showtimeId = context.getJobDetail().getJobDataMap().getString("showtimeId");

        // 1. Cập nhật trạng thái ghế sau khi giờ chiếu kết thúc
        seatService.updateSeatStatusAfterShowtime(showtimeId);


    }
}
