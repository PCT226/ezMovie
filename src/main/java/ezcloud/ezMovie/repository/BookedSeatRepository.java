package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.enities.BookedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookedSeatRepository extends JpaRepository<BookedSeat,Integer> {
}
