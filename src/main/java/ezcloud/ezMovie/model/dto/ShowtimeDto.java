package ezcloud.ezMovie.model.dto;



import lombok.Getter;
import lombok.Setter;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
@Getter
@Setter
public class ShowtimeDto {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private MovieInfo movieInfo;
    private ScreenDto screen;
}
