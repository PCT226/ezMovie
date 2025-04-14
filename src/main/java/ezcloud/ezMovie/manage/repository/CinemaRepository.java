package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Cinema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Integer> {
    Page<Cinema> findAllByIsDeleted(boolean isdeleted, Pageable pageable);
}
