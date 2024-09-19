package ezcloud.ezMovie.model.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateScreenRequest {
    private Integer cinemaId;
    private Integer screenNumber;
    private Integer capacity;
}
