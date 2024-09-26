package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat,Integer> {
    List<Seat> findAllByScreenIdAndIsDeletedFalse(int screenId);
    Seat getSeatById(int id);
}
