package ezcloud.ezMovie.booking.controller;

import ezcloud.ezMovie.booking.model.dto.TicketDto;
import ezcloud.ezMovie.booking.model.payload.BookingRequestDTO;
import ezcloud.ezMovie.booking.service.TicketService;
import ezcloud.ezMovie.exception.TicketHeldException;
import ezcloud.ezMovie.manage.model.dto.SeatDto;
import ezcloud.ezMovie.manage.model.dto.TicketAdminDto;
import ezcloud.ezMovie.manage.model.enities.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "ticket")
@Tag(name = "Ticket", description = "APIs for booking ticket")
public class TicketController {
    @Autowired
    private TicketService ticketService;

    @Operation(summary = "Book tickets", description = "Place a booking for the given tickets.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking successful",
                    content = @Content(examples = @ExampleObject(value = "{ \"success\": \"2122c81d-432b-4780-87d3-ec4b152af85d\" }"))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Tickets have been booked or held or Invalid discount code\n\n\" }"))),
            @ApiResponse(responseCode = "404", description = "Showtime or seats not found",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Seat or Showtime not found\" }"))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    @PostMapping("/booking")
    public ResponseEntity<?> bookTickets(@RequestBody @Valid BookingRequestDTO bookingRequest) {
        try {
            Response<String> ticket = ticketService.reserveSeats(
                    bookingRequest.getUserId(),
                    bookingRequest.getShowtimeId(),
                    bookingRequest.getSeatIds(),
                    bookingRequest.getDiscountCode()
            );
            return ResponseEntity.ok(ticket);
        } catch (TicketHeldException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tickets have been booked or held");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @Operation(summary = "Book tickets", description = "View Booking History.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking successful",
                    content = @Content(examples = @ExampleObject(value =
                            "{ \n" +
                                    "    \"orderId\": \"21c1d9ff-b9a1-4020-93a8-c685f41730ed\",\n" +
                                    "    \"totalPrice\": \"30000000\",\n" +
                                    "    \"orderInfo\": \"1\",\n" +
                                    "    \"paymentTime\": \"20241016110055\",\n" +
                                    "    \"message\": \"Payment Success\",\n" +
                                    "    \"transactionId\": \"14616134\",\n" +
                                    "    \"status\": \"success\"\n" +
                                    "}"))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }"))),
            @ApiResponse(responseCode = "404", description = "Showtime or seats not found",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Users have no tickets booked\" }"))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    @GetMapping("/getHistory/{id}")
    public ResponseEntity<?> getHistory(@PathVariable UUID id) {
        try {
            List<TicketDto> tickets = ticketService.findAllByUserId(id);
            return ResponseEntity.ok(tickets);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/getTempTicketInfo/{id}")
    public ResponseEntity<?> getSeatsFromRedis(@PathVariable String id) {
        try {
            List<SeatDto> tickets = ticketService.getTempTicketInfo(id);
            return ResponseEntity.ok(tickets);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Generate ticket code", description = "Generate and save a unique ticket code for a specific ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket code generated successfully",
                    content = @Content(examples = @ExampleObject(value = "{ \"ticketCode\": \"ABC123\" }"))),
            @ApiResponse(responseCode = "404", description = "Ticket not found",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Ticket not found\" }"))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    @PostMapping("/generate-code/{ticketId}")
    public ResponseEntity<?> generateTicketCode(@PathVariable UUID ticketId) {
        try {
            String ticketCode = ticketService.generateAndSaveTicketCode(ticketId);
            return ResponseEntity.ok(ticketCode);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Get all tickets with pagination", description = "Retrieve a paginated list of all tickets")
    public ResponseEntity<?> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "bookingTime") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
            Page<TicketAdminDto> tickets = ticketService.findAllTickets(pageRequest);
            return ResponseEntity.ok(new Response<>(0, tickets));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new Response<>(1, e.getMessage()));
        }
    }

    @PostMapping("/verify/{ticketCode}")
    @Operation(summary = "Verify and mark ticket as used", description = "Verify ticket code and mark ticket as used if valid")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket verified and marked as used successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid ticket code or ticket already used"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<?> verifyAndMarkTicketAsUsed(@PathVariable String ticketCode) {
        try {
            TicketDto verifiedTicket = ticketService.verifyAndMarkTicketAsUsed(ticketCode);
            return ResponseEntity.ok(new Response<>(0, verifiedTicket));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new Response<>(1, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response<>(1, "An unexpected error occurred"));
        }
    }
}
