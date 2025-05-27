package ezcloud.ezMovie.manage.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CinemaRevenueDTO {
    private Integer cinemaId;
    private String name;
    private BigDecimal revenue;
    private String address;
}