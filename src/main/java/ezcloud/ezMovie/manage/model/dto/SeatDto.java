package ezcloud.ezMovie.manage.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class SeatDto {
    private Integer seatId;
    private String seatNumber;
    private BigDecimal price;
    private String status; // "AVAILABLE", "BOOKED", "HOLD"
}
