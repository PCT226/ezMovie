package ezcloud.ezMovie.manage.model.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMovieRequest {
    private Integer id;
    private String cast;
    private String director;
    private String title;
    private String description;
    private String genre;
    private Integer duration;
    private String releaseDate;
    private String imgSrc;
}
