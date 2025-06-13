package ezcloud.ezMovie.manage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieRatingDto {
    private Integer movieId;
    private String movieTitle;
    private Double averageRating;
    private Integer totalRatings;
    private Integer userRating; // Rating của user hiện tại (nếu có)
} 