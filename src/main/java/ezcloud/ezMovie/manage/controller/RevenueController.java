package ezcloud.ezMovie.manage.controller;

import ezcloud.ezMovie.manage.model.dto.DashboardRevenueResponse;
import ezcloud.ezMovie.manage.model.dto.RevenueResponse;
import ezcloud.ezMovie.manage.model.enities.Cinema;
import ezcloud.ezMovie.manage.model.enities.Movie;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.repository.CinemaRepository;
import ezcloud.ezMovie.manage.repository.MovieRepository;
import ezcloud.ezMovie.payment.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/revenue")
@RequiredArgsConstructor
public class RevenueController {
    private final RevenueService revenueService;
    private final MovieRepository movieRepository;
    private final CinemaRepository cinemaRepository;

    @GetMapping("/dashboard")
    public Response<DashboardRevenueResponse> getDashboardRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        DashboardRevenueResponse response = revenueService.getDashboardRevenue(startDate, endDate);
        return new Response<>(0, response);
    }

    @GetMapping("/movie/{movieId}")
    public Response<RevenueResponse> getRevenueByMovie(
            @PathVariable Integer movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        Map<LocalDate, BigDecimal> dailyRevenue = revenueService.getRevenueByMovie(movie, startDate, endDate);
        BigDecimal totalRevenue = revenueService.getTotalRevenueByMovie(movie, startDate, endDate);

        RevenueResponse response = new RevenueResponse();
        response.setDailyRevenue(dailyRevenue);
        response.setTotalRevenue(totalRevenue);
        response.setMovieTitle(movie.getTitle());

        return new Response<>(0, response);
    }

    @GetMapping("/cinema/{cinemaId}")
    public Response<RevenueResponse> getRevenueByCinema(
            @PathVariable Integer cinemaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new RuntimeException("Cinema not found"));

        Map<LocalDate, BigDecimal> dailyRevenue = revenueService.getRevenueByCinema(cinema, startDate, endDate);
        BigDecimal totalRevenue = revenueService.getTotalRevenueByCinema(cinema, startDate, endDate);

        RevenueResponse response = new RevenueResponse();
        response.setDailyRevenue(dailyRevenue);
        response.setTotalRevenue(totalRevenue);
        response.setCinemaName(cinema.getName());

        return new Response<>(0, response);
    }
} 