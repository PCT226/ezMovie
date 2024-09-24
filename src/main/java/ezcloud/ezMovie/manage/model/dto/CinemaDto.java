package ezcloud.ezMovie.manage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CinemaDto {
    private Integer id;
    private String name;
    private String location;
    private String city;
}
