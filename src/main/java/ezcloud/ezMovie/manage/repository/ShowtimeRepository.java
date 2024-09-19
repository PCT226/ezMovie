package ezcloud.ezMovie.manage.repository;

import ezcloud.ezMovie.manage.model.enities.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime,Integer> {
    // Tìm các suất chiếu theo ngày và thời gian bắt đầu chưa bắt đầu
    List<Showtime> findByDateAndStartTimeAfterAndIsDeletedFalse(LocalDate date, LocalTime startTime);

    // Tìm các suất chiếu sau ngày hiện tại
    List<Showtime> findByDateAfterAndIsDeletedFalse(LocalDate date);

    // Tìm các suất chiếu của bộ phim theo ngày và thời gian bắt đầu chưa bắt đầu
    List<Showtime> findByMovieIdAndDateAndStartTimeAfterAndIsDeletedFalse(Integer movieId, LocalDate date, LocalTime startTime);

    // Tìm các suất chiếu của bộ phim sau ngày hiện tại
    List<Showtime> findByMovieIdAndDateAfterAndIsDeletedFalse(Integer movieId, LocalDate date);
}