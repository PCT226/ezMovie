package ezcloud.ezMovie.payment.controller;

import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.payment.service.VNPAYService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "payment")
@Tag(name = "Payment", description = "API for Payments")
public class PaymentController {
    @Autowired
    private VNPAYService vnPayService;

    @PostMapping("/submitOrder")
    @Operation(summary = "Submit an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order submitted successfully",
                content = @Content(examples = @ExampleObject(value = "{ \"responseCode\": 0,\n" +
                        "  \"data\": \"urlString\"}"))),
            @ApiResponse(responseCode = "404", description = "Not found ticket",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Not found ticket\" }"))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public Response<String> submitOrder(@RequestParam("TempTicketId") String id,
                                        HttpServletRequest request) {
        return vnPayService.submitOrder(request, id);
    }

    @GetMapping("/vnpay-payment-return")
    public Response<Map<String, Object>> paymentCompleted(HttpServletRequest request) {
        return vnPayService.paymentCompleted(request);
    }
    //http://localhost:8080/payment/qrGen
//    @PostMapping("/payment/qrGen")
//    public Map<String, String> qrGen(@RequestParam("TempTicketId") String id,
//                                              @RequestParam("orderInfo") String orderInfo,
//                                           HttpServletRequest request) {
//        return vnPayService.submitOrder(request,id, orderInfo);
//    }
}
 