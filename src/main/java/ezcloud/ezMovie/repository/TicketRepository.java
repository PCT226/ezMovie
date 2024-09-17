package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.enities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,Integer> {
    List<Ticket> findAllByUserId(UUID userId);
}
