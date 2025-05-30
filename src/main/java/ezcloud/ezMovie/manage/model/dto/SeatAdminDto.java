package ezcloud.ezMovie.manage.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SeatAdminDto {
    private Integer seatId;
    private String seatNumber;
    private BigDecimal price;
    private String status;
    private String screenName;
    private Integer screenId;
    private String cinemaName;
}
