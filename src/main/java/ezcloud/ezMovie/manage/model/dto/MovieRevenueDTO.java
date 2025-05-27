package ezcloud.ezMovie.manage.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MovieRevenueDTO {
    private Integer movieId;
    private String title;
    private BigDecimal revenue;
    private String poster;
}