package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Cinema;
import ezcloud.ezMovie.manage.model.enities.Movie;
import ezcloud.ezMovie.manage.model.enities.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Integer> {
    Optional<Revenue> findByCinemaAndMovieAndDate(Cinema cinema, Movie movie, LocalDate date);

    @Query("SELECT DISTINCT r FROM Revenue r WHERE r.movie = ?1 AND r.date BETWEEN ?2 AND ?3 AND r.isDeleted = false")
    List<Revenue> findByMovieAndDateBetween(Movie movie, LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT r FROM Revenue r WHERE r.cinema = ?1 AND r.date BETWEEN ?2 AND ?3 AND r.isDeleted = false")
    List<Revenue> findByCinemaAndDateBetween(Cinema cinema, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(DISTINCT r.amount) FROM Revenue r WHERE r.movie = ?1 AND r.date BETWEEN ?2 AND ?3 AND r.isDeleted = false")
    BigDecimal getTotalRevenueByMovie(Movie movie, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(DISTINCT r.amount) FROM Revenue r WHERE r.cinema = ?1 AND r.date BETWEEN ?2 AND ?3 AND r.isDeleted = false")
    BigDecimal getTotalRevenueByCinema(Cinema cinema, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(DISTINCT r.amount) FROM Revenue r WHERE r.date BETWEEN ?1 AND ?2 AND r.isDeleted = false")
    BigDecimal getTotalRevenue(LocalDate startDate, LocalDate endDate);

    @Query("SELECT r.movie, SUM(DISTINCT r.amount) as total FROM Revenue r WHERE r.date BETWEEN ?1 AND ?2 AND r.isDeleted = false GROUP BY r.movie ORDER BY total DESC")
    List<Object[]> getTopMoviesByRevenue(LocalDate startDate, LocalDate endDate);

    @Query("SELECT r.cinema, SUM(DISTINCT r.amount) as total FROM Revenue r WHERE r.date BETWEEN ?1 AND ?2 AND r.isDeleted = false GROUP BY r.cinema ORDER BY total DESC")
    List<Object[]> getTopCinemasByRevenue(LocalDate startDate, LocalDate endDate);
}
