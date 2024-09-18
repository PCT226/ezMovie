package ezcloud.ezMovie.manage.model.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateSeatRequest {
    private String seatNumber;
    private BigDecimal price;
    private Integer screenId;

}
