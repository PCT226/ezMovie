package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.model.dto.TicketDto;
import ezcloud.ezMovie.model.enities.Ticket;
import ezcloud.ezMovie.model.payload.BookingRequestDTO;
import ezcloud.ezMovie.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "ticket")
public class TicketController {
    @Autowired
    private TicketService ticketService;

    @Operation(summary = "Book tickets", description = "Place a booking for the given tickets.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Showtime or seats not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/book")
    public ResponseEntity<?> bookTickets(@RequestBody @Valid BookingRequestDTO bookingRequest) {
        try {
            TicketDto ticket = ticketService.bookTickets(
                    bookingRequest.getUserId(),
                    bookingRequest.getShowtimeId(),
                    bookingRequest.getSeatIds(),
                    bookingRequest.getDiscountCode()
            );
            return ResponseEntity.ok(ticket);
        } catch (RuntimeException e) {
            // Xử lý lỗi và trả về thông báo lỗi
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Xử lý lỗi không mong muốn
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
    @Operation(summary = "Book tickets", description = "Place a booking for the given tickets.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Showtime or seats not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/getHistory/{id}")
    public ResponseEntity<?> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.findAllByUserId(id));
    }
}