package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Showtime;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {
    // Tìm các suất chiếu theo ngày và thời gian bắt đầu chưa bắt đầu
    List<Showtime> findByDateAndStartTimeAfterAndIsDeletedFalse(LocalDate date, LocalTime startTime);

    // Tìm các suất chiếu sau ngày hiện tại
    List<Showtime> findByDateAfterAndIsDeletedFalse(LocalDate date);

    @Query("SELECT s FROM Showtime s WHERE s.date = :nowDate AND s.endTime < :nowTime")
    List<Showtime> findShowtimeByDateEqualsAndEndTimeBefore(
            @Param("nowDate") LocalDate nowDate,
            @Param("nowTime") LocalTime nowTime
    );

    List<Showtime> findByMovieIdAndDateAndStartTimeAfterAndIsDeletedFalse(Integer movieId, LocalDate date, LocalTime startTime);

    // Tìm các suất chiếu của bộ phim sau ngày hiện tại
    List<Showtime> findByMovieIdAndDateAfterAndIsDeletedFalse(Integer movieId, LocalDate date);

    List<Showtime> findAllByScreenIdAndDate(Integer screen_id, LocalDate date);

    @Query("SELECT s FROM Showtime s " +
            "WHERE s.screen.cinema.id = :cinemaId " +
            "AND s.date = :date " +
            "AND s.endTime > :currentTime " +
            "AND s.isDeleted = false")
    List<Showtime> findByCinemaIdAndDateAndEndTimeAfter(
            @Param("cinemaId") Integer cinemaId,
            @Param("date") LocalDate date,
            @Param("currentTime") LocalTime currentTime);

    List<Showtime> findByScreen_Cinema_IdAndDateAndStartTimeAfterAndIsDeletedFalse(
            Integer cinemaId, LocalDate date, LocalTime endTime);

    List<Showtime> findByScreen_Cinema_IdAndDateAfterAndIsDeletedFalse(Integer movieId, LocalDate date);

}
