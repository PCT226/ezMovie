package ezcloud.ezMovie.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenDto {
    private Integer id;
    private Integer screenNumber;
    private Integer capacity;
    private CinemaDto cinemaDto;
}
