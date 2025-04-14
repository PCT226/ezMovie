package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Integer>, JpaSpecificationExecutor<Movie> {
    Page<Movie> findAllByIsDeletedFalse(Pageable pageable);

}
