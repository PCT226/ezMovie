package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.enities.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime,Integer> {
}
