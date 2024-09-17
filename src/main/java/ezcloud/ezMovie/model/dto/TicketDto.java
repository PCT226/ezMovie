package ezcloud.ezMovie.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
public class TicketDto {
    private LocalDateTime bookingTime;
    private BigDecimal totalPrice;
    private String paymentStatus;
    private ShowtimeDto showtime;
    private UserInfo userInfo;
}
