package ezcloud.ezMovie.manage.model.payload;

import ezcloud.ezMovie.manage.model.dto.TimeDto;
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

