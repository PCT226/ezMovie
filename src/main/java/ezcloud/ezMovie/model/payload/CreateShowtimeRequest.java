package ezcloud.ezMovie.model.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import ezcloud.ezMovie.model.dto.MovieInfo;
import ezcloud.ezMovie.model.dto.ScreenDto;
import ezcloud.ezMovie.model.dto.TimeDto;
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
    private TimeDto startTime;
    private TimeDto endTime;
}

