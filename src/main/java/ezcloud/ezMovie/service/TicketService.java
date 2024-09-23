package ezcloud.ezMovie.service;

import ezcloud.ezMovie.model.dto.*;
import ezcloud.ezMovie.model.enities.*;
import ezcloud.ezMovie.model.payload.BookingRequestDTO;
import ezcloud.ezMovie.repository.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private RedisTemplate<String, Object> redisTemplate;

//    private static final String TEMP_TICKET_KEY_PREFIX = "ticket:temp:";
    private static final String SEAT_STATUS_KEY_PREFIX = "seat:status:";
    private static final String BOOKED_STATUS = "BOOKED";
    private static final String HOLD_STATUS = "HOLD";
    private static final String AVAILABLE_STATUS = "AVAILABLE";
    private static final int HOLD_TIMEOUT = 600; // Giữ ghế trong 5 phút



    @Transactional
    public String reserveSeats(UUID userId, Integer showtimeId, List<Integer> seatIds, String discountCode) {
        // Lấy thông tin suất chiếu và người dùng
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Giữ ghế tạm thời
        List<Integer> heldSeatIds;
        try {
            heldSeatIds = holdSeats(seatIds, HOLD_TIMEOUT);
        } catch (RuntimeException e) {
            // Xử lý lỗi nếu không thể giữ ghế và khôi phục trạng thái ghế nếu cần
            throw new RuntimeException("Failed to hold seats: " + e.getMessage());
        }

        // Tính toán tổng tiền
        BigDecimal totalPrice = calculateTotalPrice(seatIds, discountCode);
        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setTotalPrice(totalPrice);
        ticket.setBookingTime(LocalDateTime.now());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(null);  // Chưa cập nhật
        ticket.setDeleted(false);
        ticket.setPaymentStatus("FALSE");
        ticket.setShowtime(showtime);

        ticket = ticketRepository.save(ticket);


        // Tạo vé tạm thời và lưu vào Redis
        String tempTicketId = String.valueOf(ticket.getId());
        TempTicket tempTicket = new TempTicket(userId,showtimeId,totalPrice,heldSeatIds,discountCode,false);
        redisTemplate.opsForValue().set(tempTicketId, tempTicket, HOLD_TIMEOUT, TimeUnit.SECONDS);
        return tempTicketId;
    }

    @Transactional
    public TicketDto confirmBooking(String tempTicketId) {
        String id=tempTicketId;
        // Lấy thông tin vé tạm thời từ Redis
        TempTicket tempTicket = (TempTicket) redisTemplate.opsForValue().get(id);

        if (tempTicket == null) {
            throw new RuntimeException("Temporary ticket not found or expired");
        }
        tempTicket.setStatus(true);
        redisTemplate.opsForValue().set(tempTicketId,tempTicket);
        if(!tempTicket.getStatus()){
            throw new RuntimeException("NotPaid");
        }
        Showtime showtime = showtimeRepository.findById(tempTicket.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Showtime not found"));

        Ticket ticket = ticketRepository.findById(UUID.fromString(tempTicketId))
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Xác nhận đặt vé và cập nhật trạng thái ghế thành "BOOKED" với TTL
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.of(showtime.getDate(),showtime.getEndTime());
        long ttlSeconds = Duration.between(now, endTime).getSeconds();
        confirmSeatsBooking(tempTicket.getSeatIds(),ttlSeconds);

        // Lưu thông tin ghế đã đặt vào cơ sở dữ liệu
        saveBookedSeats(ticket, tempTicket.getSeatIds());

        // Xóa vé tạm thời khỏi Redis
        redisTemplate.delete(id);

        //Map DTO
        TicketDto ticketDto = mapper.map(ticket, TicketDto.class);
        if(ticket.getUser()!=null) {
            UserInfo userInfo=mapper.map(ticket.getUser(),UserInfo.class);
            ticketDto.setUserInfo(userInfo);
        } else {
            ticketDto.setUserInfo(null);
        }
        if(ticket.getShowtime()!=null){
            ShowtimeDto showtimeDto=mapper.map(ticket.getShowtime(),ShowtimeDto.class);
            if(ticket.getShowtime().getMovie() != null){
                MovieInfo movieInfo = mapper.map(ticket.getShowtime().getMovie(), MovieInfo.class);
                showtimeDto.setMovieInfo(movieInfo);
            }else {
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
        }else {
            ticketDto.setShowtime(null);
        }
        return ticketDto;
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
                if(ticket.getShowtime().getMovie() != null){
                    MovieInfo movieInfo = mapper.map(ticket.getShowtime().getMovie(), MovieInfo.class);
                    showtimeDto.setMovieInfo(movieInfo);
                }else {
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
            }else {
                ticketDto.setShowtime(null);
            }
            return ticketDto;
        }).collect(Collectors.toList());
    }

private List<Integer> holdSeats(List<Integer> seatIds, long holdTime) {
    List<Integer> heldSeatIds = new ArrayList<>();

    for (Integer seatId : seatIds) {
        String lockKey = "lock:" + SEAT_STATUS_KEY_PREFIX + seatId;
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 30, TimeUnit.SECONDS);
        if (lockAcquired) {
            try {
                String seatKey = SEAT_STATUS_KEY_PREFIX + seatId;
//                String seatStatus = (String) redisTemplate.opsFor                                                                     Value().get(seatKey);
                byte[] seatKeySerialized = redisTemplate.getStringSerializer().serialize(seatKey);

                // Lấy trạng thái ghế từ Redis
                String seatStatus = (String) redisTemplate.execute((RedisCallback<String>) connection -> {
                    byte[] seatStatusSerialized = connection.get(seatKeySerialized);
                    return (seatStatusSerialized != null) ? redisTemplate.getStringSerializer().deserialize(seatStatusSerialized) : null;
                });

                if (seatStatus == null) {
                    seatStatus = AVAILABLE_STATUS;
                }

                if (BOOKED_STATUS.equals(seatStatus) || HOLD_STATUS.equals(seatStatus)) {
                    throw new RuntimeException("Seat " + seatId + " is already booked or held.");
                } else if (!checkRedisPing()) {
                    throw new RuntimeException("Cannot connect to Redis");
                }

//                redisTemplate.opsForValue().set(seatKey, HOLD_STATUS, holdTime, TimeUnit.SECONDS);
                // Đặt ghế vào trạng thái HOLD
                redisTemplate.execute((RedisCallback<Object>) connection -> {
                    connection.set(seatKeySerialized,
                            redisTemplate.getStringSerializer().serialize(HOLD_STATUS));
                    connection.expire(
                            seatKeySerialized,
                            HOLD_TIMEOUT
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
private void saveBookedSeats(Ticket ticket, List<Integer> heldSeatIds) {
    for (Integer seatId : heldSeatIds) {
        BookedSeat bookedSeat = new BookedSeat();
        bookedSeat.setTicket(ticket);
        bookedSeat.setSeat(seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found")));
        bookedSeatRepository.save(bookedSeat);
    }
}
    //Cập nhập trạng thái ghế đã đặt
    private void confirmSeatsBooking(List<Integer> seatIds,long ttlSeconds) {
                    redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.multi();
            for (Integer seatId : seatIds) {
                String seatStatusKey = SEAT_STATUS_KEY_PREFIX + seatId;
                connection.set(
                        redisTemplate.getStringSerializer().serialize(seatStatusKey),
                        redisTemplate.getStringSerializer().serialize(BOOKED_STATUS)
                ); // Set trạng thái BOOKED
                Seat seatOPT=seatRepository.findById(seatId)
                        .orElseThrow(()->new RuntimeException());
                seatOPT.setSeatStatus(BOOKED_STATUS);
                seatOPT.setUpdatedAt(LocalDateTime.now());


                connection.expire(
                        redisTemplate.getStringSerializer().serialize(seatStatusKey),
                        ttlSeconds
                ); // Thiết lập TTL cho trạng thái BOOKED
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
    public TempTicket getTempTicketInfo(String id){
        return (TempTicket) redisTemplate.opsForValue().get(id);
    }
    public void updateStatus(String id){
        TempTicket tempTicket= (TempTicket) redisTemplate.opsForValue().get(id);
        tempTicket.setStatus(true);
        redisTemplate.opsForValue().set(id,tempTicket);

    }


    public boolean checkRedisPing() {
        try {
            String pingResult = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equals(pingResult);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
