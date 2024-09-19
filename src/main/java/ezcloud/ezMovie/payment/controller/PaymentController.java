package ezcloud.ezMovie.payment.controller;

import ezcloud.ezMovie.model.dto.TempTicket;
import ezcloud.ezMovie.payment.service.VNPAYService;
import ezcloud.ezMovie.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "payment")
public class PaymentController {
    @Autowired
    private VNPAYService vnPayService;

    @PostMapping("/submitOrder")
    public Map<String, String> submitOrder(@RequestParam("TempTicketId") String id,
                                           @RequestParam("orderInfo") String orderInfo,
                                           HttpServletRequest request) {
        return vnPayService.submitOrder(request,id, orderInfo);
    }

    @GetMapping("/vnpay-payment-return")
    public Map<String, Object> paymentCompleted(HttpServletRequest request) {
        return vnPayService.paymentCompleted(request);
    }

}
