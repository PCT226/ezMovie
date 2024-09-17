package ezcloud.ezMovie.model.payload;

import ezcloud.ezMovie.model.enities.Screen;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateSeatRequest {
    private int seatId;
    private String seatNumber;
    private BigDecimal price;
    private Screen screen;
}
