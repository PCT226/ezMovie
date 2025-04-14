package ezcloud.ezMovie.manage.model.payload;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class UpdateShowtimeRq {
    private Integer showtimeId;
    private Integer movieId;
    private Integer screenId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
}
