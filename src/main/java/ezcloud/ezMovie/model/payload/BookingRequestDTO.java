package ezcloud.ezMovie.model.payload;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
@Getter
@Setter
public class BookingRequestDTO {
    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @NotNull(message = "Showtime ID cannot be null")
    @Positive(message = "Showtime ID must be a positive number")
    private Integer showtimeId;

    @NotEmpty(message = "Seat IDs cannot be empty")
    private List<@NotNull(message = "Seat ID cannot be null") @Positive(message = "Seat ID must be a positive number") Integer> seatIds;

    private String discountCode;

}
