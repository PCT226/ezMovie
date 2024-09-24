package ezcloud.ezMovie.repository;

import ezcloud.ezMovie.model.dto.MovieInfo;
import ezcloud.ezMovie.model.enities.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie,Integer>, JpaSpecificationExecutor<Movie> {
    List<Movie> findAllByIsDeletedFalse();

}
