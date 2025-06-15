package ezcloud.ezMovie.booking.service;

import ezcloud.ezMovie.auth.model.dto.UserInfo;
import ezcloud.ezMovie.auth.model.enities.User;
import ezcloud.ezMovie.auth.repository.UserRepository;
import ezcloud.ezMovie.auth.service.EmailService;
import ezcloud.ezMovie.booking.model.dto.TempTicket;
import ezcloud.ezMovie.booking.model.dto.TicketDto;
import ezcloud.ezMovie.booking.model.enities.BookedSeat;
import ezcloud.ezMovie.booking.model.enities.Discount;
import ezcloud.ezMovie.booking.model.enities.Ticket;
import ezcloud.ezMovie.booking.repository.BookedSeatRepository;
import ezcloud.ezMovie.booking.repository.DiscountRepository;
import ezcloud.ezMovie.booking.repository.TicketRepository;
import ezcloud.ezMovie.exception.TicketHeldException;
import ezcloud.ezMovie.manage.model.dto.*;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.model.enities.Seat;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.repository.SeatRepository;
import ezcloud.ezMovie.manage.repository.ShowtimeRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    @Autowired
    private EmailService emailService;

    @Transactional
    public Response<String> reserveSeats(UUID userId, Integer showtimeId, List<Integer> seatIds, String discountCode) throws Exception {

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (seatIds.isEmpty()) {
            throw new RuntimeException("Seat not found");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.of(showtime.getDate(), showtime.getEndTime());
        long ttlSeconds = Duration.between(now, endTime).getSeconds();

        List<SeatDto> availableSeats = getTempTicketInfo(String.valueOf(showtimeId));

        for (SeatDto seat : availableSeats) {
            if (seatIds.contains(seat.getSeatId())) {
                if ("AVAILABLE".equals(seat.getStatus())) {
                    seat.setStatus("HOLD");
                } else {
                    throw new TicketHeldException("Tickets have been booked or held");
                }
            }
        }
        redisTemplate.opsForValue().set("listSeat::" + showtimeId, availableSeats);
        redisTemplate.expire("listSeat::" + showtimeId, ttlSeconds, TimeUnit.SECONDS);
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
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu vé tạm thời vào Redis: " + e.getMessage());
        }

        return new Response<>(0, tempTicketId);

    }

    @Transactional
    public TicketDto confirmBooking(String tempTicketId) {
        Ticket ticket = ticketRepository.findById(UUID.fromString(tempTicketId))
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        List<Integer> listSeatId = getSeatIdsFromRedis(tempTicketId);

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
        if (tickets.isEmpty()) {
            throw new RuntimeException("Users have no tickets booked");
        }
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

            // Map seats information
            List<BookedSeat> bookedSeats = bookedSeatRepository.findBookedSeatsByTicket_Id(ticket.getId());
            List<String> seatNumbers = bookedSeats.stream()
                .map(bookedSeat -> bookedSeat.getSeat().getSeatNumber())
                .collect(Collectors.toList());
            ticketDto.setSeats(seatNumbers);

            return ticketDto;
        }).collect(Collectors.toList());
    }

    //Lưu thông tin ghế đã đặt
    public void saveBookedSeats(Ticket ticket, List<Integer> heldSeatIds) {
        // Kiểm tra xem các ghế đã được đặt cho ticket này chưa
        List<BookedSeat> existingBookings = bookedSeatRepository.findBookedSeatsByTicket_Id(ticket.getId());
        Set<Integer> existingSeatIds = existingBookings.stream()
            .map(bookedSeat -> bookedSeat.getSeat().getId())
            .collect(Collectors.toSet());

        for (Integer seatId : heldSeatIds) {
            // Chỉ lưu nếu ghế chưa được đặt cho ticket này
            if (!existingSeatIds.contains(seatId)) {
                BookedSeat bookedSeat = new BookedSeat();
                bookedSeat.setTicket(ticket);
                bookedSeat.setSeat(seatRepository.findById(seatId)
                        .orElseThrow(() -> new RuntimeException("Seat not found")));
                bookedSeat.setCreatedAt(LocalDateTime.now());
                bookedSeat.setUpdatedAt(LocalDateTime.now());
                bookedSeatRepository.save(bookedSeat);
            }
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
        List<SeatDto> availableSeats = (List<SeatDto>) redisTemplate.opsForValue().get(key);
        return availableSeats;
    }

    public List<Integer> getSeatIdsFromRedis(String tempTicketId) {
        TempTicket tempTicketJson = (TempTicket) redisTemplate.opsForValue().get(tempTicketId);

        if (tempTicketJson == null) {
            throw new RuntimeException("TempTicket not found");
        }
        return tempTicketJson.getSeatIds();

    }

    @Transactional
    public String generateAndSaveTicketCode(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Generate a unique ticket code
        String ticketCode = generateUniqueTicketCode();

        ticket.setTicketCode(ticketCode);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Only send email if the ticket is not already confirmed
        if ("CONFIRMED".equals(ticket.getPaymentStatus())) {
            try {
                // Lấy thông tin ghế từ Redis
                List<Integer> seatIds = getSeatIdsFromRedis(String.valueOf(ticket.getId()));
                List<Seat> seats = seatRepository.findAllById(seatIds);

                String emailBody = String.format(
                        "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: linear-gradient(135deg, #1a1a1a 0%%, #2a2a2a 100%%); color: white; padding: 40px; border-radius: 16px; box-shadow: 0 10px 20px rgba(0,0,0,0.2);'>" +
                                // Logo và header
                                "<div style='text-align: center; margin-bottom: 30px;'>" +
                                "<h1 style='color: #ff4444; margin: 0; font-size: 28px; text-transform: uppercase; letter-spacing: 3px;'>E-TICKET</h1>" +
                                "<div style='width: 50px; height: 3px; background: #ff4444; margin: 15px auto;'></div>" +
                                "<p style='color: #999; margin: 10px 0; font-size: 16px;'>ezMovie Entertainment</p>" +
                                "</div>" +

                                // Mã vé
                                "<div style='background: rgba(255,255,255,0.05); padding: 25px; border-radius: 12px; margin-bottom: 30px; border: 1px solid rgba(255,255,255,0.1);'>" +
                                "<div style='text-align: center;'>" +
                                "<div style='color: #999; font-size: 14px; margin-bottom: 10px;'>MÃ VÉ CỦA BẠN</div>" +
                                "<div style='font-size: 36px; letter-spacing: 10px; color: #ff4444; font-weight: bold;'>%s</div>" +
                                "</div>" +
                                "</div>" +

                                // Thông tin phim
                                "<div style='background: rgba(255,255,255,0.05); padding: 30px; border-radius: 12px; margin-bottom: 30px; border: 1px solid rgba(255,255,255,0.1);'>" +
                                "<h2 style='color: white; margin: 0 0 20px 0; font-size: 24px; text-align: center;'>%s</h2>" +
                                "<div style='display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px;'>" +
                                "<div style='text-align: left;'>" +
                                "<div style='color: #999; font-size: 13px; margin-bottom: 5px;'>RẠP CHIẾU</div>" +
                                "<div style='color: white; font-size: 16px;'>%s</div>" +
                                "</div>" +
                                "<div style='text-align: left;'>" +
                                "<div style='color: #999; font-size: 13px; margin-bottom: 5px;'>ĐỊA CHỈ</div>" +
                                "<div style='color: white; font-size: 16px;'>%s</div>" +
                                "</div>" +
                                "</div>" +
                                "</div>" +

                                // Thông tin ghế và thời gian
                                "<div style='background: rgba(255,255,255,0.05); padding: 30px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.1);'>" +
                                "<div style='margin-bottom: 25px;'>" +
                                "<div style='color: #999; font-size: 13px; margin-bottom: 8px;'>PHÒNG</div>" +
                                "<div style='color: #ff4444; font-size: 22px; font-weight: bold;'>%s</div>" +
                                "</div>" +
                                "<div style='margin-bottom: 25px;'>" +
                                "<div style='color: #999; font-size: 13px; margin-bottom: 8px;'>GHẾ ĐÃ ĐẶT</div>" +
                                "<div style='color: #ff4444; font-size: 18px;'>" +
                                generateSeatList(seats) +
                                "</div>" +
                                "</div>" +
                                "<div style='display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; padding-top: 20px; border-top: 1px solid rgba(255,255,255,0.1);'>" +
                                "<div>" +
                                "<div style='color: #999; font-size: 13px; margin-bottom: 8px;'>NGÀY CHIẾU</div>" +
                                "<div style='color: white; font-size: 16px;'>%s</div>" +
                                "</div>" +
                                "<div>" +
                                "<div style='color: #999; font-size: 13px; margin-bottom: 8px;'>GIỜ CHIẾU</div>" +
                                "<div style='color: white; font-size: 16px;'>%s</div>" +
                                "</div>" +
                                "</div>" +
                                "</div>" +

                                // Footer
                                "<div style='text-align: center; margin-top: 30px; padding-top: 30px; border-top: 1px solid rgba(255,255,255,0.1);'>" +
                                "<p style='color: #999; font-size: 14px; margin: 5px 0;'>Vui lòng xuất trình mã vé này tại quầy vé</p>" +
                                "<p style='color: #666; font-size: 12px; margin: 10px 0;'>Mọi thắc mắc xin liên hệ: support@ezmovie.com</p>" +
                                "</div>" +
                                "</div>",
                        ticketCode,
                        ticket.getShowtime().getMovie().getTitle(),
                        ticket.getShowtime().getScreen().getCinema().getName(),
                        ticket.getShowtime().getScreen().getCinema().getLocation(),
                        ticket.getShowtime().getScreen().getScreenNumber(),
                        ticket.getShowtime().getDate(),
                        ticket.getShowtime().getStartTime()
                );

                emailService.sendEmail(
                        ticket.getUser().getEmail(),
                        "Vé điện tử - " + ticket.getShowtime().getMovie().getTitle(),
                        emailBody
                );
            } catch (MessagingException e) {
                // Log the error but don't throw it since the ticket code was still generated successfully
                System.err.println("Failed to send email: " + e.getMessage());
            }
        }

        return ticketCode;
    }

    private String getBookedSeats(Ticket ticket) {
        List<BookedSeat> bookedSeats = bookedSeatRepository.findBookedSeatsByTicket_Id(ticket.getId());
        return bookedSeats.stream()
                .map(bookedSeat -> bookedSeat.getSeat().getSeatNumber())
                .collect(Collectors.joining(", "));
    }

    private String getSeatRow(String seatNumber) {
        if (seatNumber == null || seatNumber.isEmpty()) {
            return "";
        }
        return seatNumber.substring(0, 1);
    }

    private String getSeatNumber(String seatNumber) {
        if (seatNumber == null || seatNumber.length() <= 1) {
            return "";
        }
        return seatNumber.substring(1);
    }

    private String generateUniqueTicketCode() {
        // Generate a random 6-character alphanumeric code
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * characters.length());
            code.append(characters.charAt(index));
        }
        return code.toString();
    }

    private String generateSeatList(List<Seat> seats) {
        StringBuilder seatList = new StringBuilder();
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            seatList.append(String.format(
                    "<div style='display: inline-block; margin: 5px 10px 5px 0; padding: 8px 15px; background: rgba(255,255,255,0.1); border-radius: 6px;'>" +
                            "Hàng %s - Ghế %s" +
                            "</div>",
                    getSeatRow(seat.getSeatNumber()),
                    getSeatNumber(seat.getSeatNumber())
            ));
        }
        return seatList.toString();
    }

    public Page<TicketAdminDto> findAllTickets(PageRequest pageRequest) {
        Page<Ticket> tickets = ticketRepository.findAll(pageRequest);
        return tickets.map(ticket -> {
            TicketAdminDto ticketDto = mapper.map(ticket, TicketAdminDto.class);
            ticketDto.setUsed(ticket.isUsed());
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
        });
    }

    @Transactional
    public TicketDto verifyAndMarkTicketAsUsed(String ticketCode) {
        // Tìm vé theo mã vé
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode);

        // Kiểm tra điều kiện
        if (!ticket.isPaid()) {
            throw new RuntimeException("Ticket has not been paid");
        }
        if (ticket.isUsed()) {
            throw new RuntimeException("Ticket has already been used");
        }

        // Cập nhật trạng thái sử dụng
        ticket.setUsed(true);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Tạo file vé giấy (txt)
        generatePaperTicket(ticket);

        // Chuyển đổi và trả về DTO
        TicketDto ticketDto = mapper.map(ticket, TicketDto.class);
        if (ticket.getUser() != null) {
            UserInfo userInfo = mapper.map(ticket.getUser(), UserInfo.class);
            ticketDto.setUserInfo(userInfo);
        }
        if (ticket.getShowtime() != null) {
            ShowtimeDto showtimeDto = mapper.map(ticket.getShowtime(), ShowtimeDto.class);
            if (ticket.getShowtime().getMovie() != null) {
                MovieInfo movieInfo = mapper.map(ticket.getShowtime().getMovie(), MovieInfo.class);
                showtimeDto.setMovieInfo(movieInfo);
            }
            if (ticket.getShowtime().getScreen() != null) {
                ScreenDto screenDto = mapper.map(ticket.getShowtime().getScreen(), ScreenDto.class);
                if (ticket.getShowtime().getScreen().getCinema() != null) {
                    CinemaDto cinemaDTO = mapper.map(ticket.getShowtime().getScreen().getCinema(), CinemaDto.class);
                    screenDto.setCinemaDto(cinemaDTO);
                }
                showtimeDto.setScreen(screenDto);
            }
            ticketDto.setShowtime(showtimeDto);
        }
        ticketDto.setTicketCode(ticketCode);

        // Thêm thông tin về seats
        List<BookedSeat> bookedSeats = bookedSeatRepository.findBookedSeatsByTicket_Id(ticket.getId());
        List<String> seatNumbers = bookedSeats.stream()
            .map(bookedSeat -> bookedSeat.getSeat().getSeatNumber())
            .collect(Collectors.toList());
        ticketDto.setSeats(seatNumbers);

        return ticketDto;
    }

    private void generatePaperTicket(Ticket ticket) {
        try {
            // Tạo thư mục tickets nếu chưa tồn tại
            String directory = "tickets";
            if (!Files.exists(Paths.get(directory))) {
                Files.createDirectories(Paths.get(directory));
            }

            // Tạo tên file
            String fileName = String.format("%s/ticket_%s_%s.txt", 
                directory, 
                ticket.getTicketCode(), 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            // Lấy thông tin ghế
            List<BookedSeat> bookedSeats = bookedSeatRepository.findBookedSeatsByTicket_Id(ticket.getId());
            List<String> seatNumbers = bookedSeats.stream()
                .map(bookedSeat -> bookedSeat.getSeat().getSeatNumber())
                .collect(Collectors.toList());

            // Tạo nội dung vé
            StringBuilder content = new StringBuilder();
            content.append("=".repeat(50)).append("\n");
            content.append("           EZ MOVIE - MOVIE TICKET\n");
            content.append("=".repeat(50)).append("\n\n");
            
            content.append("TICKET CODE: ").append(ticket.getTicketCode()).append("\n");
            content.append("VERIFICATION TIME: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n\n");
            
            content.append("MOVIE INFORMATION:\n");
            content.append("-".repeat(30)).append("\n");
            content.append("Title: ").append(ticket.getShowtime().getMovie().getTitle()).append("\n");
            content.append("Duration: ").append(ticket.getShowtime().getMovie().getDuration()).append(" minutes\n");
            content.append("Genre: ").append(ticket.getShowtime().getMovie().getGenre()).append("\n");
            content.append("Director: ").append(ticket.getShowtime().getMovie().getDirector()).append("\n\n");
            
            content.append("SHOWTIME INFORMATION:\n");
            content.append("-".repeat(30)).append("\n");
            content.append("Date: ").append(ticket.getShowtime().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            content.append("Time: ").append(ticket.getShowtime().getStartTime()).append(" - ").append(ticket.getShowtime().getEndTime()).append("\n\n");
            
            content.append("CINEMA INFORMATION:\n");
            content.append("-".repeat(30)).append("\n");
            content.append("Cinema: ").append(ticket.getShowtime().getScreen().getCinema().getName()).append("\n");
            content.append("Address: ").append(ticket.getShowtime().getScreen().getCinema().getLocation()).append("\n");
            content.append("Screen: ").append(ticket.getShowtime().getScreen().getScreenNumber()).append("\n\n");
            
            content.append("SEAT INFORMATION:\n");
            content.append("-".repeat(30)).append("\n");
            content.append("Seats: ").append(String.join(", ", seatNumbers)).append("\n");
            content.append("Number of seats: ").append(seatNumbers.size()).append("\n\n");
            
            content.append("CUSTOMER INFORMATION:\n");
            content.append("-".repeat(30)).append("\n");
            content.append("Name: ").append(ticket.getUser().getUsername()).append("\n");
            content.append("Email: ").append(ticket.getUser().getEmail()).append("\n");
            content.append("Phone: ").append(ticket.getUser().getPhoneNumber()).append("\n\n");
            
            content.append("PAYMENT INFORMATION:\n");
            content.append("-".repeat(30)).append("\n");
            content.append("Booking time: ").append(ticket.getBookingTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
            content.append("Total amount: ").append(ticket.getTotalPrice()).append(" VND\n");
            content.append("Payment status: ").append(ticket.getPaymentStatus()).append("\n");
            content.append("Ticket status: USED\n\n");
            
            content.append("IMPORTANT NOTES:\n");
            content.append("-".repeat(30)).append("\n");
            content.append("1. Please arrive 15 minutes before the showtime\n");
            content.append("2. Present this ticket at the entrance\n");
            content.append("3. No refunds or exchanges\n");
            content.append("4. Food and drinks are not allowed in the theater\n");
            content.append("5. Please turn off your phone during the movie\n\n");
            
            content.append("=".repeat(50)).append("\n");
            content.append("           THANK YOU FOR CHOOSING EZ MOVIE!\n");
            content.append("=".repeat(50)).append("\n");

            // Ghi file
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(content.toString());
            }

            System.out.println("Paper ticket generated: " + fileName);
        } catch (IOException e) {
            System.err.println("Error generating paper ticket: " + e.getMessage());
        }
    }
}

