package ezcloud.ezMovie.manage.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class RevenueResponse {
    private Map<LocalDate, BigDecimal> dailyRevenue;
    private BigDecimal totalRevenue;
    private String movieTitle;
    private String cinemaName;
} 