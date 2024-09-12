package ezcloud.ezMovie.model.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateScreenRequest {
    private Integer screenId;
    private Integer cinemaId;  // Không bắt buộc, chỉ cập nhật nếu cần
    private Integer screenNumber;
    private Integer capacity;

}
