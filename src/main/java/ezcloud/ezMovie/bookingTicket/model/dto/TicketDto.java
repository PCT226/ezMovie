package ezcloud.ezMovie.bookingTicket.model.dto;

import ezcloud.ezMovie.manage.model.dto.ShowtimeDto;
import ezcloud.ezMovie.auth.model.dto.UserInfo;
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
