package ezcloud.ezMovie.payment.service;

import ezcloud.ezMovie.manage.model.dto.CinemaRevenueDTO;
import ezcloud.ezMovie.manage.model.dto.DashboardRevenueResponse;
import ezcloud.ezMovie.manage.model.dto.MovieRevenueDTO;
import ezcloud.ezMovie.manage.model.enities.Cinema;
import ezcloud.ezMovie.manage.model.enities.Movie;
import ezcloud.ezMovie.manage.model.enities.Revenue;
import ezcloud.ezMovie.manage.repository.RevenueRepository;
import ezcloud.ezMovie.booking.repository.TicketRepository;
import ezcloud.ezMovie.auth.repository.UserRepository;
import ezcloud.ezMovie.manage.repository.MovieRepository;
import ezcloud.ezMovie.manage.repository.ShowtimeRepository;
import ezcloud.ezMovie.manage.model.dto.RecentTicketDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueService {
    private final RevenueRepository revenueRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    @Transactional
    public void addRevenue(Cinema cinema, Movie movie, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        
        // Tìm revenue hiện tại của cinema và movie trong ngày
        Revenue existingRevenue = revenueRepository.findByCinemaAndMovieAndDate(cinema, movie, today)
                .orElse(new Revenue());

        // Nếu chưa có revenue cho ngày này, tạo mới
        if (existingRevenue.getId() == null) {
            existingRevenue.setCinema(cinema);
            existingRevenue.setMovie(movie);
            existingRevenue.setDate(today);
            existingRevenue.setAmount(amount);
            existingRevenue.setCreatedAt(LocalDateTime.now());
        } else {
            // Nếu đã có, cộng thêm amount
            existingRevenue.setAmount(existingRevenue.getAmount().add(amount));
            existingRevenue.setUpdatedAt(LocalDateTime.now());
        }

        revenueRepository.save(existingRevenue);
    }

    public Map<LocalDate, BigDecimal> getRevenueByMovie(Movie movie, LocalDate startDate, LocalDate endDate) {
        List<Revenue> revenues = revenueRepository.findByMovieAndDateBetween(movie, startDate, endDate);
        return revenues.stream()
                .collect(Collectors.groupingBy(
                    Revenue::getDate,
                    Collectors.reducing(
                        BigDecimal.ZERO,
                        Revenue::getAmount,
                        BigDecimal::add
                    )
                ));
    }

    public Map<LocalDate, BigDecimal> getRevenueByCinema(Cinema cinema, LocalDate startDate, LocalDate endDate) {
        List<Revenue> revenues = revenueRepository.findByCinemaAndDateBetween(cinema, startDate, endDate);
        return revenues.stream()
                .collect(Collectors.groupingBy(
                    Revenue::getDate,
                    Collectors.reducing(
                        BigDecimal.ZERO,
                        Revenue::getAmount,
                        BigDecimal::add
                    )
                ));
    }

    public BigDecimal getTotalRevenueByMovie(Movie movie, LocalDate startDate, LocalDate endDate) {
        return revenueRepository.getTotalRevenueByMovie(movie, startDate, endDate);
    }

    public BigDecimal getTotalRevenueByCinema(Cinema cinema, LocalDate startDate, LocalDate endDate) {
        return revenueRepository.getTotalRevenueByCinema(cinema, startDate, endDate);
    }

    public DashboardRevenueResponse getDashboardRevenue(LocalDate startDate, LocalDate endDate) {
        DashboardRevenueResponse response = new DashboardRevenueResponse();

        // Get total revenue
        response.setTotalRevenue(revenueRepository.getTotalRevenue(startDate, endDate));

        // Get revenue by movie
        List<Object[]> movieRevenues = revenueRepository.getTopMoviesByRevenue(startDate, endDate);
        Map<String, BigDecimal> revenueByMovie = new HashMap<>();
        List<MovieRevenueDTO> topMovies = new ArrayList<>();

        for (Object[] result : movieRevenues) {
            Movie movie = (Movie) result[0];
            BigDecimal revenue = (BigDecimal) result[1];
            revenueByMovie.put(movie.getTitle(), revenue);

            MovieRevenueDTO dto = new MovieRevenueDTO();
            dto.setMovieId(movie.getId());
            dto.setTitle(movie.getTitle());
            dto.setRevenue(revenue);
            dto.setPoster(movie.getImgSrc());
            topMovies.add(dto);
        }
        response.setRevenueByMovie(revenueByMovie);
        response.setTopMovies(topMovies);

        // Get revenue by cinema
        List<Object[]> cinemaRevenues = revenueRepository.getTopCinemasByRevenue(startDate, endDate);
        Map<String, BigDecimal> revenueByCinema = new HashMap<>();
        List<CinemaRevenueDTO> topCinemas = new ArrayList<>();

        for (Object[] result : cinemaRevenues) {
            Cinema cinema = (Cinema) result[0];
            BigDecimal revenue = (BigDecimal) result[1];
            revenueByCinema.put(cinema.getName(), revenue);

            CinemaRevenueDTO dto = new CinemaRevenueDTO();
            dto.setCinemaId(cinema.getId());
            dto.setName(cinema.getName());
            dto.setRevenue(revenue);
            dto.setAddress(cinema.getLocation());
            topCinemas.add(dto);
        }
        response.setRevenueByCinema(revenueByCinema);
        response.setTopCinemas(topCinemas);

        // Tổng quan
        response.setTotalMovies((int) movieRepository.count());
        response.setTotalShowtimes((int) showtimeRepository.count());
        response.setTotalTickets((int) ticketRepository.count());
        response.setTotalUsers((int) userRepository.count());

        // Recent tickets (10 vé mới nhất)
        var recentTickets = ticketRepository.findTop10ByOrderByBookingTimeDesc();
        List<RecentTicketDTO> recentTicketDTOs = new ArrayList<>();
        for (var ticket : recentTickets) {
            RecentTicketDTO dto = new RecentTicketDTO();
            dto.setMovieTitle(ticket.getShowtime() != null && ticket.getShowtime().getMovie() != null ? ticket.getShowtime().getMovie().getTitle() : "");
            dto.setShowtime(ticket.getBookingTime() != null ? ticket.getBookingTime().toString() : "");
            dto.setStatus(ticket.getPaymentStatus());
            recentTicketDTOs.add(dto);
        }
        response.setRecentTickets(recentTicketDTOs);

        return response;
    }
} 