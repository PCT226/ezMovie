package ezcloud.ezMovie.booking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezcloud.ezMovie.auth.model.dto.UserInfo;
import ezcloud.ezMovie.auth.model.enities.User;
import ezcloud.ezMovie.auth.repository.UserRepository;
import ezcloud.ezMovie.booking.model.dto.TempTicket;
import ezcloud.ezMovie.booking.model.dto.TicketDto;
import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import ezcloud.ezMovie.booking.model.enities.Discount;
import ezcloud.ezMovie.booking.model.enities.Ticket;
import ezcloud.ezMovie.booking.repository.BookedSeatRepository;
import ezcloud.ezMovie.booking.repository.DiscountRepository;
import ezcloud.ezMovie.booking.repository.TicketRepository;
import ezcloud.ezMovie.manage.model.dto.*;
import ezcloud.ezMovie.manage.model.enities.Seat;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.repository.SeatRepository;
import ezcloud.ezMovie.manage.repository.ShowtimeRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TicketService {
    private static final int HOLD_TIMEOUT = 900;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private DiscountRepository discountRepository;
    @Autowired
    private BookedSeatRepository bookedSeatRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper mapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Getter
    private List<Integer> listSeatId;
    @Transactional
    public String reserveSeats(UUID userId, Integer showtimeId, List<Integer> seatIds, String discountCode) throws Exception {

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.of(showtime.getDate(),showtime.getEndTime());
        long ttlSeconds = Duration.between(now, endTime).getSeconds();

        List<SeatDto> availableSeats = getTempTicketInfo(String.valueOf(showtimeId));

        for (SeatDto seat : availableSeats) {
            if (seatIds.contains(seat.getSeatId())) {
                if ("AVAILABLE".equals(seat.getStatus())) {
                    seat.setStatus("HOLD");
                }else{
                    throw new RuntimeException("Vé đã được đặt hoặc giữ");
                }
            }
        }
        redisTemplate.opsForValue().set("listSeat::"+showtimeId, availableSeats);
        redisTemplate.expire("listSeat::"+showtimeId, ttlSeconds, TimeUnit.SECONDS);

        listSeatId = seatIds;
        // Tính toán tổng tiền
        BigDecimal totalPrice = calculateTotalPrice(seatIds, discountCode);
        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setTotalPrice(totalPrice);
        ticket.setBookingTime(LocalDateTime.now());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(null); // Chưa cập nhật
        ticket.setDeleted(false);
        ticket.setPaymentStatus("FALSE");
        ticket.setShowtime(showtime);

        ticketRepository.save(ticket);

//        // Tạo vé tạm thời và lưu vào Redis
        String tempTicketId = String.valueOf(ticket.getId());
        TempTicket tempTicket = new TempTicket(userId, showtimeId, totalPrice, seatIds, discountCode, false);
        try {
            redisTemplate.opsForValue().set(tempTicketId, tempTicket, HOLD_TIMEOUT, TimeUnit.SECONDS);
        }catch (Exception e) {
            System.err.println("Lỗi khi lưu vé tạm thời vào Redis: " + e.getMessage());
        }

        return tempTicketId;

    }

    @Transactional
    public TicketDto confirmBooking(String tempTicketId) {
        Ticket ticket = ticketRepository.findById(UUID.fromString(tempTicketId))
                .orElseThrow(() -> new RuntimeException("Ticket not found"));


        saveBookedSeats(ticket, listSeatId);

        TicketDto ticketDto = mapper.map(ticket, TicketDto.class);
        if (ticket.getUser() != null) {
            UserInfo userInfo = mapper.map(ticket.getUser(), UserInfo.class);
            ticketDto.setUserInfo(userInfo);
        } else {
            ticketDto.setUserInfo(null);
        }
        if (ticket.getShowtime() != null) {
            ShowtimeDto showtimeDto = mapper.map(ticket.getShowtime(), ShowtimeDto.class);
            if (ticket.getShowtime().getMovie() != null) {
                MovieInfo movieInfo = mapper.map(ticket.getShowtime().getMovie(), MovieInfo.class);
                showtimeDto.setMovieInfo(movieInfo);
            } else {
                showtimeDto.setMovieInfo(null);
            }
            if (ticket.getShowtime().getScreen() != null) {
                ScreenDto screenDto = mapper.map(ticket.getShowtime().getScreen(), ScreenDto.class);
                if (ticket.getShowtime().getScreen().getCinema() != null) {
                    CinemaDto cinemaDTO = mapper.map(ticket.getShowtime().getScreen().getCinema(), CinemaDto.class);
                    screenDto.setCinemaDto(cinemaDTO);
                } else {
                    screenDto.setCinemaDto(null);
                }
                showtimeDto.setScreen(screenDto);
            } else {
                showtimeDto.setScreen(null);
            }
            ticketDto.setShowtime(showtimeDto);
        } else {
            ticketDto.setShowtime(null);
        }
        return ticketDto;
    }

    public List<TicketDto> findAllByUserId(UUID userId) {
        List<Ticket> tickets = ticketRepository.findAllByUserId(userId);
        return tickets.stream().map(ticket -> {
            TicketDto ticketDto = mapper.map(ticket, TicketDto.class);
            if (ticket.getUser() != null) {
                UserInfo userInfo = mapper.map(ticket.getUser(), UserInfo.class);
                ticketDto.setUserInfo(userInfo);
            } else {
                ticketDto.setUserInfo(null);
            }
            if (ticket.getShowtime() != null) {
                ShowtimeDto showtimeDto = mapper.map(ticket.getShowtime(), ShowtimeDto.class);
                if (ticket.getShowtime().getMovie() != null) {
                    MovieInfo movieInfo = mapper.map(ticket.getShowtime().getMovie(), MovieInfo.class);
                    showtimeDto.setMovieInfo(movieInfo);
                } else {
                    showtimeDto.setMovieInfo(null);
                }
                if (ticket.getShowtime().getScreen() != null) {
                    ScreenDto screenDto = mapper.map(ticket.getShowtime().getScreen(), ScreenDto.class);
                    if (ticket.getShowtime().getScreen().getCinema() != null) {
                        CinemaDto cinemaDTO = mapper.map(ticket.getShowtime().getScreen().getCinema(), CinemaDto.class);
                        screenDto.setCinemaDto(cinemaDTO);
                    } else {
                        screenDto.setCinemaDto(null);
                    }
                    showtimeDto.setScreen(screenDto);
                } else {
                    showtimeDto.setScreen(null);
                }
                ticketDto.setShowtime(showtimeDto);
            } else {
                ticketDto.setShowtime(null);
            }
            return ticketDto;
        }).collect(Collectors.toList());
    }

    //Lưu thông tin ghế đã đặt
    public void saveBookedSeats(Ticket ticket, List<Integer> heldSeatIds) {
        for (Integer seatId : heldSeatIds) {
            BookedSeat bookedSeat = new BookedSeat();
            bookedSeat.setTicket(ticket);
            bookedSeat.setSeat(seatRepository.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Seat not found")));
            bookedSeatRepository.save(bookedSeat);
        }
    }
    private BigDecimal calculateTotalPrice(List<Integer> seatIds, String discountCode) {
        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new RuntimeException("Some seats not found.");
        }
        BigDecimal totalPrice = seats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Áp dụng mã giảm giá nếu có
        if (discountCode != null && !discountCode.isEmpty()) {
            Discount discount = discountRepository.findByCode(discountCode)
                    .orElseThrow(() -> new RuntimeException("Invalid discount code"));
            BigDecimal discountAmount = totalPrice.multiply(discount.getPercentage()).divide(BigDecimal.valueOf(100));
            totalPrice = totalPrice.subtract(discountAmount);
        }
        return totalPrice;
    }

    public List<SeatDto> getTempTicketInfo(String id) {
        String key = "listSeat::" + id;
        List<SeatDto>  availableSeats=(List<SeatDto>) redisTemplate.opsForValue().get(key);
        return availableSeats;
    }

}
