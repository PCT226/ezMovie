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
    //private static final String TEMP_TICKET_KEY_PREFIX = "ticket:temp:";
    private static final String SEAT_STATUS_KEY_PREFIX = "seat:status:";
    private static final String BOOKED_STATUS = "BOOKED";
    private static final String HOLD_STATUS = "HOLD";
    private static final String AVAILABLE_STATUS = "AVAILABLE";
    private static final int HOLD_TIMEOUT = 600; // Giữ ghế trong 10 phút
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
    private List<Integer> listSeatId;
    @Transactional
    public String reserveSeats(UUID userId, Integer showtimeId, List<Integer> seatIds, String discountCode) throws Exception {

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.of(showtime.getDate(), showtime.getEndTime());
        long ttlSeconds = Duration.between(now, endTime).getSeconds();

        List<SeatDto> availableSeats = getAvailableSeatsByShowtime(showtimeId,ttlSeconds);

        if (availableSeats.isEmpty()) {
            throw new RuntimeException("Seats available not found");
        }

        List<Integer> availableSeatIds = null;
        try {
            availableSeatIds = availableSeats.stream()
                    .map(SeatDto::getSeatId)
                    .collect(Collectors.toList());
        }catch (Exception e){
            e.printStackTrace();
        }

        for (Integer seatId : seatIds) {
            if (!availableSeatIds.contains(seatId)) {
                throw new RuntimeException("Seat with id " + seatId + " in Showtime with id "+showtimeId+" is not available.");
            }
        }
        listSeatId = seatIds;
//        // Giữ ghế tạm thời
//        List<Integer> heldSeatIds;
//        try {
//            heldSeatIds = holdSeats(seatIds, HOLD_TIMEOUT, showtimeId);
//        } catch (RuntimeException e) {
//            throw new RuntimeException("Failed to hold seats: " + e.getMessage());
//        }

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
        try{
            TempTicket tempTicket = (TempTicket) redisTemplate.opsForValue().get(tempTicketId);
            if (tempTicket == null) {
                throw new RuntimeException("Temporary ticket not found or expired");
            }

            String redisKey = String.valueOf(tempTicket.getShowtimeId());
            List<Object> availableSeats = (List<Object>) redisTemplate.opsForValue().get(redisKey);

            if (availableSeats != null) {
                Iterator<Object> iterator = availableSeats.iterator();
                while (iterator.hasNext()) {
                    SeatDto seat = (SeatDto) iterator.next();
                    if (tempTicket.getSeatIds().contains(seat.getSeatId())) {
                        iterator.remove();
                    }
                }
                Long currentTTL = redisTemplate.getExpire(redisKey);

                redisTemplate.opsForValue().set(redisKey, availableSeats);
                redisTemplate.expire(redisKey, Duration.ofSeconds(currentTTL));
            }
            redisTemplate.delete(tempTicketId);
        }catch(Exception e){
            e.printStackTrace();
        }
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

    private List<Integer> holdSeats(List<Integer> seatIds, long holdTime, Integer showtimeId) {
        List<Integer> heldSeatIds = new ArrayList<>();

        for (Integer seatId : seatIds) {
            String lockKey = "lock:" + SEAT_STATUS_KEY_PREFIX + seatId + "+" + showtimeId;
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 30, TimeUnit.SECONDS);
            if (lockAcquired) {
                try {
                    String seatKey = SEAT_STATUS_KEY_PREFIX + seatId + "+" + showtimeId;
//                String seatStatus = (String) redisTemplate.opsForValue().get(seatKey);
                    byte[] seatKeySerialized = redisTemplate.getStringSerializer().serialize(seatKey);

                    // Lấy trạng thái ghế từ Redis
                    String seatStatus = redisTemplate.execute((RedisCallback<String>) connection -> {
                        byte[] seatStatusSerialized = connection.get(seatKeySerialized);
                        return (seatStatusSerialized != null) ? redisTemplate.getStringSerializer().deserialize(seatStatusSerialized) : null;
                    });

                    if (seatStatus == null) {
                        seatStatus = AVAILABLE_STATUS;
                    }

                    if (BOOKED_STATUS.equals(seatStatus) || HOLD_STATUS.equals(seatStatus)) {
                        throw new RuntimeException("Seat " + seatId + "in Show " + showtimeId + " is already booked or held.");
                    }

//                redisTemplate.opsForValue().set(seatKey, HOLD_STATUS, holdTime, TimeUnit.SECONDS);
                    // Đặt ghế vào trạng thái HOLD
                    redisTemplate.execute((RedisCallback<Object>) connection -> {
                        connection.set(seatKeySerialized,
                                redisTemplate.getStringSerializer().serialize(HOLD_STATUS));
                        connection.expire(
                                seatKeySerialized,
                                holdTime
                        );
                        return null;
                    });
                    heldSeatIds.add(seatId);
                } finally {
                    redisTemplate.delete(lockKey);  // Giải phóng khóa
                }
            } else {
                throw new RuntimeException("Unable to acquire lock for seat " + seatId);
            }
        }

        return heldSeatIds;
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

    //Cập nhập trạng thái ghế đã đặt
    private void confirmSeatsBooking(List<Integer> seatIds, long ttlSeconds) {

        redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.multi();
            for (Integer seatId : seatIds) {
                String seatStatusKey = SEAT_STATUS_KEY_PREFIX + seatId;
                connection.set(
                        redisTemplate.getStringSerializer().serialize(seatStatusKey),
                        redisTemplate.getStringSerializer().serialize(BOOKED_STATUS)
                ); // Set trạng thái BOOKED
                connection.expire(
                        redisTemplate.getStringSerializer().serialize(seatStatusKey),
                        ttlSeconds
                ); // Thiết lập TTL cho trạng thái BOOKED
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new RuntimeException("Seat not found"));
//                seat.setSeatStatus(BOOKED_STATUS);
                seat.setUpdatedAt(LocalDateTime.now());
            }
            connection.exec();
            return true;
        });
    }

    //Tính tổng giá
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

    public TempTicket getTempTicketInfo(String id) {
        return (TempTicket) redisTemplate.opsForValue().get(id);
    }

    public void updateStatus(String id) {
        TempTicket tempTicket = (TempTicket) redisTemplate.opsForValue().get(id);
        tempTicket.setStatus(true);
        redisTemplate.opsForValue().set(id, tempTicket);

    }
    public List<SeatDto> getAvailableSeatsByShowtime(Integer showtimeId,long ttlSeconds) {
        String showtimeid = String.valueOf(showtimeId);
        List<SeatDto> availableSeats = null;
        try {
            availableSeats=(List<SeatDto>) redisTemplate.opsForValue().get(showtimeid);
        }catch (RedisConnectionFailureException e){
                //removeSeatsCache(showtimeId);
            System.err.println("Lỗi khi truy cập Redis: " + e.getMessage());
        }

        if (availableSeats == null) {
            List<Seat> seats = seatRepository.findAvailableSeatsByShowtime(showtimeId);
            availableSeats = seats.stream()
                    .map(seat -> mapper.map(seat, SeatDto.class))
                    .collect(Collectors.toList());
            try {
                redisTemplate.opsForValue().set(showtimeid, availableSeats, Duration.ofSeconds(ttlSeconds));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return availableSeats;
    }


    // Xóa danh sách ghế trong cache khi có thay đổi đặt ghế
    public void removeSeatsCache(Integer showtimeId) {
        String redisKey = String.valueOf(showtimeId);
        redisTemplate.delete(redisKey);
    }

    public boolean isRedisAlive() {
        try {
            // Gửi lệnh PING đến Redis
            String response = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equals(response);
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Redis không hoạt động
        }
    }
}
