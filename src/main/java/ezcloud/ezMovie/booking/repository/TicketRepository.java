package ezcloud.ezMovie.booking.repository;

import ezcloud.ezMovie.booking.model.enities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;


@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findAllByUserId(UUID userId);
    List<Ticket> findAllByShowtime_Id(Integer id);
}
