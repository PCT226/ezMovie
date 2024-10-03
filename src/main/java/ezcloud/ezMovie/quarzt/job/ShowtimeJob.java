package ezcloud.ezMovie.quarzt.job;
import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import ezcloud.ezMovie.booking.model.enities.Ticket;
import ezcloud.ezMovie.booking.repository.BookedSeatRepository;
import ezcloud.ezMovie.booking.repository.TicketRepository;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.service.ShowtimeService;
import jakarta.transaction.Transactional;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
@Component
public class ShowtimeJob implements Job {

    @Autowired
    private ShowtimeService showtimeService;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private BookedSeatRepository bookedSeatRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Showtime> showtimes = showtimeService.getShowtimeOutTime();

        for (Showtime showtime : showtimes) {
            int showtimeId=showtime.getId();
            showtime.setDeleted(true);
            List<BookedSeat> bookedSeats =bookedSeatRepository.findBookedSeatsByShowtimeId(showtimeId);
            for (BookedSeat bookedSeat:bookedSeats){
                bookedSeat.setDeleted(true);
            }
            List<Ticket> tickets = ticketRepository.findAllByShowtime_Id(showtimeId);
            for (Ticket ticket:tickets){
                ticket.setDeleted(true);
            }

        }
    }


}
