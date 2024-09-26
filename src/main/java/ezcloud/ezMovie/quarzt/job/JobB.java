package ezcloud.ezMovie.quarzt.job;

import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import ezcloud.ezMovie.booking.model.enities.Ticket;
import ezcloud.ezMovie.booking.repository.BookedSeatRepository;
import ezcloud.ezMovie.booking.repository.TicketRepository;
import ezcloud.ezMovie.manage.model.enities.Seat;
import ezcloud.ezMovie.manage.repository.SeatRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class JobB implements Job {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private BookedSeatRepository bookedSeatRepository;
    @Override
    public void execute(JobExecutionContext context) {
        Integer showtimeId = context.getMergedJobDataMap().getInt("showtimeId");
        List<Ticket> tickets = ticketRepository.findAllByShowtime_Id(showtimeId);
        for(Ticket ticket:tickets){
            List<BookedSeat> bookedSeats = bookedSeatRepository.getAllByTicket_Id(ticket.getId());

            for (BookedSeat bookedSeat : bookedSeats) {
                Seat seat = seatRepository.getSeatById(bookedSeat.getSeat().getId());
                seat.setSeatStatus("AVAILABLE");
                seatRepository.save(seat);
            }
        }
        System.out.println("IN HERE");
    }
}
