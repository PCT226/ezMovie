package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.dto.CinemaDto;
import ezcloud.ezMovie.model.enities.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema,Integer> {
    List<Cinema> findAllByIsDeleted(boolean isdeleted);
}
