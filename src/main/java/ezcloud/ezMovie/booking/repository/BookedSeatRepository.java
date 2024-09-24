package ezcloud.ezMovie.booking.repository;

import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookedSeatRepository extends JpaRepository<BookedSeat,Integer> {
}
