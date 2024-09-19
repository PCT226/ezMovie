package ezcloud.ezMovie.manage.model.dto;



import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ShowtimeDto {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private MovieInfo movieInfo;
    private ScreenDto screen;
}
