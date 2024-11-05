package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Seat;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat,Integer> {
    List<Seat> findAllByScreenIdAndIsDeletedFalse(int screenId);
    List<Seat> findAllByScreenIdAndIsDeletedFalse(int screenId, Pageable pageable);
    @Query("SELECT s FROM Seat s " +
            "JOIN Screen scr ON s.screen.id = scr.id " +
            "JOIN Showtime st ON st.screen.id = scr.id " +
            "LEFT JOIN BookedSeat bs ON s.id = bs.seat.id AND bs.ticket.id IN " +
            "(SELECT t.id FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.isDeleted = false ) " +
            "WHERE st.id = :showtimeId " +
            "AND bs.seat.id IS NULL " +
            "AND (bs.isDeleted = false OR bs IS NULL)")
    List<Seat> findAvailableSeatsByShowtime(@Param("showtimeId") Integer showtimeId);

    Seat getSeatById(int id);
}
