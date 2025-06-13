package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    
    @Query("SELECT c FROM Comment c WHERE c.movie.id = :movieId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByMovieIdOrderByCreatedAtDesc(@Param("movieId") Integer movieId, Pageable pageable);
    
    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.movie.id = :movieId AND c.isDeleted = false")
    Long countByMovieId(@Param("movieId") Integer movieId);
    
    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.isDeleted = false")
    Comment findByIdAndNotDeleted(@Param("id") Integer id);
} 