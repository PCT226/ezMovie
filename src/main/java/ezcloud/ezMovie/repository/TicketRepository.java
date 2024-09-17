package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.enities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;


@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findAllByUserId(UUID userId);
}
