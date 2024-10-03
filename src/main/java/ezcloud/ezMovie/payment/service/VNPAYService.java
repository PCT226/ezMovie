package ezcloud.ezMovie.payment.service;

import ezcloud.ezMovie.booking.model.dto.TempTicket;
import ezcloud.ezMovie.booking.model.enities.Ticket;
import ezcloud.ezMovie.manage.model.enities.Showtime;
import ezcloud.ezMovie.manage.repository.ShowtimeRepository;
import ezcloud.ezMovie.payment.config.VNPAYConfig;
import ezcloud.ezMovie.booking.repository.TicketRepository;
import ezcloud.ezMovie.booking.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

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

    private String orderId;

    public Map<String, String> submitOrder(HttpServletRequest request, String id, String orderInfo) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

        //int orderTotal = ticketService.getTempTicketInfo(id).getTotalPrice().intValue();
        int orderTotal = ticketRepository.getTicketById(UUID.fromString(id)).getTotalPrice().intValue();
        String vnpayUrl = createOrder(request, orderTotal, orderInfo, baseUrl);

        saveOrder(id,orderInfo);
        orderId = id;
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", vnpayUrl);
        return response;
    }

    public Map<String, Object> paymentCompleted(HttpServletRequest request) {
        int paymentStatus = orderReturn(request);
        // Lấy thông tin từ request
        String orderInfo = request.getParameter("vnp_OrderInfo");
        String paymentTime = request.getParameter("vnp_PayDate");
        String transactionId = request.getParameter("vnp_TransactionNo");
        String totalPrice = request.getParameter("vnp_Amount");

        // Chuẩn bị dữ liệu trả về
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("orderInfo",orderInfo);
        response.put("totalPrice", totalPrice);
        response.put("paymentTime", paymentTime);
        response.put("transactionId", transactionId);

        if (paymentStatus == 1) {
            // Nếu thanh toán thành công, cập nhật isPaid = true
            updatePaymentStatus(UUID.fromString(orderId));
            ticketService.confirmBooking(orderId);
//            ticketService.removeSeatsCache(showtimeId);
//            ticketService.getAvailableSeatsByShowtime(showtimeId,ttlSeconds);
            //ticketService.updateStatus(orderId);
            //saveOrder(orderInfo);
            response.put("message", "Payment Success");
            response.put("status", "success");
        } else {
            response.put("message", "Payment Failed");
            response.put("status", "failed");
        }

        return response;
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

        String locate = "vn";
        vnp_Params.put("vnp_Locale", locate);

        urlReturn += VNPAYConfig.vnp_Returnurl;
        vnp_Params.put("vnp_ReturnUrl", urlReturn);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String salt = VNPAYConfig.vnp_HashSecret;
        String vnp_SecureHash = VNPAYConfig.hmacSHA512(salt, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPAYConfig.vnp_PayUrl + "?" + queryUrl;

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
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }
        String signValue = VNPAYConfig.hashAllFields(fields);
        if (signValue.equals(vnp_SecureHash)) {
            return "00".equals(request.getParameter("vnp_TransactionStatus")) ? 1 : 0;
        } else {
            return -1;
        }
    }



    public void saveOrder(String id, String orderInfo) {

        Optional<Ticket> ticketOpt = ticketRepository.findById(UUID.fromString(id));

        if (ticketOpt.isPresent()) {
            Ticket ticket = ticketOpt.get();
            ticket.setOrderInfo(orderInfo);
            ticket.setPaid(false);
            ticketRepository.save(ticket);
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
