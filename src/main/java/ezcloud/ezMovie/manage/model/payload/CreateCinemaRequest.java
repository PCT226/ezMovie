package ezcloud.ezMovie.manage.model.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCinemaRequest {
    private String name;
    private String location;
    private String city;
}
