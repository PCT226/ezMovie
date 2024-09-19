package ezcloud.ezMovie.bookingTicket.controller;

import ezcloud.ezMovie.bookingTicket.model.dto.TicketDto;
import ezcloud.ezMovie.bookingTicket.model.payload.BookingRequestDTO;
import ezcloud.ezMovie.bookingTicket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    @PostMapping("/booking")
    public ResponseEntity<?> bookTickets(@RequestBody @Valid BookingRequestDTO bookingRequest) {
        try {
            String ticket = ticketService.reserveSeats(
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
    @PostMapping("/confirm-booking/{tempId}")
    public ResponseEntity<?> confirmBooking(@PathVariable String tempId) {
        try {
            TicketDto ticketDto = ticketService.confirmBooking(tempId);
            return ResponseEntity.ok(ticketDto);
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
        try {
            List<TicketDto> tickets = ticketService.findAllByUserId(id);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
