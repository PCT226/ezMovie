package ezcloud.ezMovie.booking.repository;

import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookedSeatRepository extends JpaRepository<BookedSeat,Integer> {
    List<BookedSeat> getAllByTicket_Id(UUID uuid);
}
