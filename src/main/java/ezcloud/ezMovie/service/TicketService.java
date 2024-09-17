package ezcloud.ezMovie.service;

import ezcloud.ezMovie.model.dto.*;
import ezcloud.ezMovie.model.enities.*;
import ezcloud.ezMovie.repository.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketService {
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
    private RedisTemplate<String, String> redisTemplate;

    private static final String SEAT_STATUS_KEY_PREFIX = "seat_status_";
    private static final String BOOKED_STATUS = "BOOKED";
    private static final String HOLD_STATUS = "HOLD";
    private static final String AVAILABLE_STATUS = "AVAILABLE";
    private static final int HOLD_TIMEOUT = 300; // Giữ ghế trong 5 phút

    // Giữ ghế trong Redis
    private List<Integer> holdSeats(List<Integer> seatIds) {
        List<Integer> heldSeatIds = new ArrayList<>();
        redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.multi();
            for (Integer seatId : seatIds) {
                byte[] seatKey = redisTemplate.getStringSerializer().serialize(SEAT_STATUS_KEY_PREFIX + seatId);
                connection.get(seatKey);  // Lấy trạng thái ghế
            }
            List<Object> results = connection.exec();  // Hoàn thành việc kiểm tra trạng thái

            for (int i = 0; i < results.size(); i++) {
                byte[] seatStatusBytes = (byte[]) results.get(i);
                String seatStatus = seatStatusBytes != null ? new String(seatStatusBytes) : AVAILABLE_STATUS;
                if (BOOKED_STATUS.equals(seatStatus) || HOLD_STATUS.equals(seatStatus)) {
                    throw new RuntimeException("Seat " + seatIds.get(i) + " is already booked or held.");
                }
            }

            // Giữ ghế nếu tất cả đều trống
            connection.multi();
            for (Integer seatId : seatIds) {
                String seatStatusKey = SEAT_STATUS_KEY_PREFIX + seatId;
                connection.set(
                        redisTemplate.getStringSerializer().serialize(seatStatusKey),
                        redisTemplate.getStringSerializer().serialize(HOLD_STATUS)
                ); // Set ghế vào trạng thái HOLD
                connection.expire(
                        redisTemplate.getStringSerializer().serialize(seatStatusKey),
                        HOLD_TIMEOUT
                ); // Giữ ghế trong HOLD_TIMEOUT giây
                heldSeatIds.add(seatId); // Lưu danh sách ghế đã giữ
            }
            connection.exec();
            return true;
        });

        return heldSeatIds;
    }

    // Xác nhận đặt ghế và xử lý thanh toán
    private Ticket confirmBooking(UUID userId, Integer showtimeId, List<Integer> seatIds, String discountCode, long ttlSeconds, List<Integer> heldSeatIds) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        User user= userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tính tổng tiền
        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new RuntimeException("Some seats not found.");
        }
        BigDecimal totalPrice = seats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Áp dụng mã giảm giá
        if (discountCode != null && !discountCode.isEmpty()) {
            Discount discount = discountRepository.findByCode(discountCode)
                    .orElseThrow(() -> new RuntimeException("Invalid discount code"));
            BigDecimal discountAmount = totalPrice.multiply(discount.getPercentage()).divide(BigDecimal.valueOf(100));
            totalPrice = totalPrice.subtract(discountAmount);
        }

        // Tạo và lưu vé
        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setShowtime(showtime);
        ticket.setTotalPrice(totalPrice);
        ticket.setPaymentStatus("PENDING"); // Chờ thanh toán
        ticket.setBookingTime(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        // Lưu thông tin ghế đã đặt và chuyển trạng thái thành "BOOKED"

        redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.multi();
            for (Integer seatId : heldSeatIds) {
                String seatStatusKey = SEAT_STATUS_KEY_PREFIX + seatId;
                connection.set(
                        redisTemplate.getStringSerializer().serialize(seatStatusKey),
                        redisTemplate.getStringSerializer().serialize(BOOKED_STATUS)
                ); // Set trạng thái BOOKED
                connection.expire(
                        redisTemplate.getStringSerializer().serialize(seatStatusKey),
                        ttlSeconds
                ); // Set TTL cho đến khi suất chiếu kết thúc

            }
            connection.exec();
            return true;
        });
        // Lưu thông tin ghế đã đặt với vé
        for (Integer seatId : heldSeatIds) {
            BookedSeat bookedSeat = new BookedSeat();
            bookedSeat.setTicket(ticket);
            bookedSeat.setSeat(seatRepository.findById(seatId).orElseThrow(() -> new RuntimeException("Seat not found")));
            bookedSeatRepository.save(bookedSeat);
        }

        return ticket;
    }

    @Transactional
    public TicketDto bookTickets(UUID userId, Integer showtimeId, List<Integer> seatIds, String discountCode) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

        // Tính toán TTL dựa trên thời gian kết thúc suất chiếu
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime showtimeEnd = LocalDateTime.of(showtime.getDate(),showtime.getEndTime());
        long ttlSeconds = Duration.between(now, showtimeEnd).getSeconds();

        List<Integer> heldSeatIds = new ArrayList<>();
        Boolean transactionSuccess = false;

        try {
            // Bước 1: Giữ ghế trong Redis
            heldSeatIds = holdSeats(seatIds);

            // Bước 2: Xác nhận đặt vé và xử lý thanh toán
            Ticket ticket = confirmBooking(userId, showtimeId, seatIds, discountCode, ttlSeconds, heldSeatIds);

            transactionSuccess = true;

            //Map Dto
            TicketDto ticketDto=mapper.map(ticket, TicketDto.class);
            if(ticket.getUser()!=null) {
                UserInfo userInfo=mapper.map(ticket.getUser(),UserInfo.class);
                ticketDto.setUserInfo(userInfo);
            } else {
                ticketDto.setUserInfo(null);
            }
            if(ticket.getShowtime()!=null){
                ShowtimeDto showtimeDto=mapper.map(ticket.getShowtime(),ShowtimeDto.class);
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
            }else {
                ticketDto.setShowtime(null);
            }
            return ticketDto;
        } catch (Exception e) {
            // Rollback: Giải phóng ghế nếu xảy ra lỗi
            if (!transactionSuccess) {
                for (Integer seatId : heldSeatIds) {
                    String seatStatusKey = SEAT_STATUS_KEY_PREFIX + seatId;
                    redisTemplate.delete(seatStatusKey); // Giải phóng ghế bị giữ
                }
            }
            throw new RuntimeException("Booking failed: " + e.getMessage(), e);
        }
    }

    public List<TicketDto> findAllByUserId(UUID userId){
        List<Ticket> tickets=ticketRepository.findAllByUserId(userId);
        //Map DTO
        return tickets.stream().map(ticket -> {
            TicketDto ticketDto=mapper.map(ticket, TicketDto.class);
            if(ticket.getUser()!=null) {
                UserInfo userInfo=mapper.map(ticket.getUser(),UserInfo.class);
                ticketDto.setUserInfo(userInfo);
            } else {
                ticketDto.setUserInfo(null);
            }
            if(ticket.getShowtime()!=null){
                ShowtimeDto showtimeDto=mapper.map(ticket.getShowtime(),ShowtimeDto.class);
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
            }else {
                ticketDto.setShowtime(null);
            }
            return ticketDto;
        }).collect(Collectors.toList());
    }
}
