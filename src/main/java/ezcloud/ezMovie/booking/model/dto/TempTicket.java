package ezcloud.ezMovie.booking.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TempTicket implements Serializable {
    private UUID userId;
    private Integer showtimeId;
    private BigDecimal totalPrice;
    private List<Integer> seatIds;
    private String discountCode;
    private Boolean status = false;
}
