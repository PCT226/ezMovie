package ezcloud.ezMovie.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Setter
@Getter
public class TimeDto {
    private int hour;
    private int minute;
    private int second;
    private int nano;

    // Getters and setters

    public LocalTime toLocalTime() {
        return LocalTime.of(hour, minute, second, nano);
    }
}
