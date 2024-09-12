package ezcloud.ezMovie.model.payload;

import ezcloud.ezMovie.model.dto.MovieInfo;
import ezcloud.ezMovie.model.dto.ScreenDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
@Getter
@Setter
public class CreateShowtimeRequest {
    private Integer movieId;
    private Integer screenId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
}

