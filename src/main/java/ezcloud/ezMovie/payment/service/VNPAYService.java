package ezcloud.ezMovie.payment.service;

import ezcloud.ezMovie.booking.model.enities.Ticket;
import ezcloud.ezMovie.booking.repository.TicketRepository;
import ezcloud.ezMovie.booking.service.TicketService;
import ezcloud.ezMovie.manage.model.dto.SeatDto;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.repository.ShowtimeRepository;
import ezcloud.ezMovie.payment.config.VNPAYConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class VNPAYService {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private TicketService ticketService;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RevenueService revenueService;

    public Response<String> submitOrder(HttpServletRequest request, String id) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        int orderTotal = ticketRepository.getTicketById(UUID.fromString(id)).getTotalPrice().intValue();
        String vnpayUrl = createOrder(request, orderTotal, id, baseUrl);

        saveOrder(id);
        return new Response<>(0, vnpayUrl);
    }


    public Integer paymentCompleted(String orderId, int paymentStatus) {

        // Lấy thông tin từ request

        Ticket ticket = ticketRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new RuntimeException("Ticket not found"));


        Integer showtimeId = ticket.getShowtime().getId();
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.of(showtime.getDate(), showtime.getEndTime());
        long ttlSeconds = Duration.between(now, endTime).getSeconds();

        List<SeatDto> availableSeats = ticketService.getTempTicketInfo(String.valueOf(showtimeId));

        for (SeatDto seat : availableSeats) {
            if (ticketService.getSeatIdsFromRedis(orderId).contains(seat.getSeatId())) {
                if (paymentStatus == 1) {
                    seat.setStatus("BOOKED");
                } else {
                    seat.setStatus("AVAILABLE");
                }
            }
        }
        redisTemplate.opsForValue().set("listSeat::" + showtimeId, availableSeats, ttlSeconds, TimeUnit.SECONDS);
        int resCode;
        if (paymentStatus == 1) {
            // Nếu thanh toán thành công
            if (!ticket.isPaid()) {
                Showtime showtimeRev = showtimeRepository.findById(ticket.getShowtime().getId())
                    .orElse(null);
                if (showtimeRev != null) {
                    revenueService.addRevenue(
                        showtimeRev.getScreen().getCinema(),
                        showtimeRev.getMovie(),
                        new BigDecimal(ticket.getTotalPrice().toString())
                    );
                }
                updatePaymentStatus(UUID.fromString(orderId));
            }
            String ticketCode = ticketService.generateAndSaveTicketCode(UUID.fromString(orderId));
            ticketService.confirmBooking(orderId);
            resCode = 0;
        } else {
            resCode = 1;
        }

        return resCode;
    }

    public String createOrder(HttpServletRequest request, int amount, String orderInfor, String urlReturn) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = VNPAYConfig.getRandomNumber(8);
        String vnp_IpAddr = VNPAYConfig.getIpAddress(request);
        String vnp_TmnCode = VNPAYConfig.vnp_TmnCode;
        String orderType = "order-type";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfor);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPAYConfig.vnp_Returnurl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(tz);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(tz);

        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        Calendar expire = (Calendar) cld.clone();
        expire.add(Calendar.MINUTE, 10);
        String vnp_ExpireDate = formatter.format(expire.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Generate hash data
        String hashData = VNPAYConfig.hashAllFields(vnp_Params);
        System.out.println("Generated hash: " + hashData); // Debug log
        vnp_Params.put("vnp_SecureHash", hashData);

        // Build query URL
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length() > 0) {
                query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
                query.append('&');
            }
        }
        String queryUrl = query.toString();
        if (queryUrl.endsWith("&")) {
            queryUrl = queryUrl.substring(0, queryUrl.length() - 1);
        }
        String paymentUrl = VNPAYConfig.vnp_PayUrl + "?" + queryUrl;
        System.out.println("Payment URL: " + paymentUrl); // Debug log

        return paymentUrl;
    }

    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");
        String signValue = VNPAYConfig.hashAllFields(fields);
        if (signValue.equals(vnp_SecureHash)) {
            return "00".equals(request.getParameter("vnp_TransactionStatus")) ? 1 : 0;
        } else {
            return -1;
        }
    }


    public void saveOrder(String id) {

        Optional<Ticket> ticketOpt = ticketRepository.findById(UUID.fromString(id));

        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            ticket.setPaid(false);
            ticketRepository.save(ticket);
        } else {
            throw new NotFoundException("Not found ticket");
        }
    }

    public void updatePaymentStatus(UUID revenueId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(revenueId);

        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            ticket.setPaymentStatus("CONFIRMED");
            ticket.setUpdatedAt(LocalDateTime.now());
            ticket.setPaid(true);
            ticketRepository.save(ticket);
        }
    }


}
