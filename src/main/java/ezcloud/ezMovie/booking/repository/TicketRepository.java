package ezcloud.ezMovie.booking.repository;

import ezcloud.ezMovie.booking.model.enities.Ticket;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findAllByUserId(UUID userId);

    List<Ticket> findAllByShowtime_Id(Integer id);

    Ticket getTicketById(UUID id);

    Ticket findByTicketCode(String ticketCode);


    @Query("SELECT t.showtime.id FROM Ticket t WHERE t.id = :ticketId")
    Integer getShowtimeIdByTicketId(@Param("ticketId") UUID ticketId);

    List<Ticket> findTop10ByOrderByBookingTimeDesc();
}
