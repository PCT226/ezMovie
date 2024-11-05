package ezcloud.ezMovie.manage.model.enities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderResponse {

    private String vnpayUrl;  // URL trả về
    private String orderId;    // ID đơn hàng

}