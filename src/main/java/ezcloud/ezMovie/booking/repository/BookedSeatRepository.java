package ezcloud.ezMovie.booking.repository;

import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookedSeatRepository extends JpaRepository<BookedSeat, Integer> {
    List<BookedSeat> getAllByTicket_Id(UUID uuid);

    @Query("SELECT bs FROM BookedSeat bs " +
            "JOIN bs.seat s " +
            "JOIN bs.ticket t " +
            "WHERE t.showtime.id = :showtimeId " +
            "AND bs.isDeleted = false " +
            "AND t.isDeleted = false")
    List<BookedSeat> findBookedSeatsByShowtimeId(@Param("showtimeId") int showtimeId);

    List<BookedSeat> findBookedSeatsByTicket_Id(UUID ticketId);
}
