package ezcloud.ezMovie.manage.model.dto;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class TicketConfirmationRequest {
    // Getter và Setter
    private String tempTicketId;
    private List<Integer> seatIds;

    public void setTempTicketId(String tempTicketId) {
        this.tempTicketId = tempTicketId;
    }

    public void setSeatIds(List<Integer> seatIds) {
        this.seatIds = seatIds;
    }
}
