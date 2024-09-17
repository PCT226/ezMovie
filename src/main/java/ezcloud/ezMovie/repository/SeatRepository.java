package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.enities.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat,Integer> {
    List<Seat> findAllByScreenIdAndIsDeletedFalse(int screenId);
}
