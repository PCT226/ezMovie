package ezcloud.ezMovie.model.payload;

import ezcloud.ezMovie.model.enities.Screen;
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
