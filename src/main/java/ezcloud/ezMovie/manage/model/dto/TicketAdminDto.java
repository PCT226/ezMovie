package ezcloud.ezMovie.manage.model.dto;

import ezcloud.ezMovie.auth.model.dto.UserInfo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TicketAdminDto {
    private LocalDateTime bookingTime;
    private BigDecimal totalPrice;
    private String paymentStatus;
    private ShowtimeDto showtime;
    private UserInfo userInfo;
    private String ticketCode;
    private List<String> seats;
    private boolean isUsed;
}
