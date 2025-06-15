package ezcloud.ezMovie.manage.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecentTicketDTO {
    private String movieTitle;
    private String showtime; // Định dạng dd/MM/yyyy HH:mm
    private String status;
} 