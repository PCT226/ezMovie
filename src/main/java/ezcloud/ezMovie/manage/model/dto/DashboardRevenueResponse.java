package ezcloud.ezMovie.manage.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DashboardRevenueResponse {
    private BigDecimal totalRevenue;
    private Map<String, BigDecimal> revenueByMovie; // movieTitle -> amount
    private Map<String, BigDecimal> revenueByCinema; // cinemaName -> amount
    private List<MovieRevenueDTO> topMovies;
    private List<CinemaRevenueDTO> topCinemas;

    // Thêm các trường tổng quan và recent tickets
    private int totalMovies;
    private int totalShowtimes;
    private int totalTickets;
    private int totalUsers;
    private List<RecentTicketDTO> recentTickets;
}



