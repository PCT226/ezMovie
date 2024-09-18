package ezcloud.ezMovie.bookingTicket.repository;

import ezcloud.ezMovie.bookingTicket.model.enities.BookedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookedSeatRepository extends JpaRepository<BookedSeat,Integer> {
}
