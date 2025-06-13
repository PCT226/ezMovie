package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {
    
    @Query("SELECT r FROM Rating r WHERE r.movie.id = :movieId AND r.user.id = :userId AND r.isDeleted = false")
    Optional<Rating> findByMovieIdAndUserId(@Param("movieId") Integer movieId, @Param("userId") UUID userId);
    
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.movie.id = :movieId AND r.isDeleted = false")
    Double getAverageRatingByMovieId(@Param("movieId") Integer movieId);
    
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.movie.id = :movieId AND r.isDeleted = false")
    Long countByMovieId(@Param("movieId") Integer movieId);
    
    @Query("SELECT r FROM Rating r WHERE r.movie.id = :movieId AND r.isDeleted = false")
    java.util.List<Rating> findAllByMovieId(@Param("movieId") Integer movieId);
} 