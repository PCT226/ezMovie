package ezcloud.ezMovie.manage.model.payload;

import ezcloud.ezMovie.manage.model.dto.TimeDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateShowtimeRequest {
    private Integer movieId;
    private Integer screenId;
    private LocalDate date;
    private TimeDto startTime;
    private TimeDto endTime;
}

