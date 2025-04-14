package ezcloud.ezMovie.manage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieInfo {
    private Integer id;

    private String actor;
    private String director;
    private String title;
    private String description;
    private String genre;
    private Integer duration;
    private String releaseDate;
    private String imgSrc;
}
