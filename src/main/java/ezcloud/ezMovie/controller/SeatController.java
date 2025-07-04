package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.manage.model.dto.SeatAdminDto;
import ezcloud.ezMovie.manage.model.dto.SeatDto;
import ezcloud.ezMovie.manage.model.enities.Seat;
import ezcloud.ezMovie.manage.model.payload.CreateSeatRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateSeatRequest;
import ezcloud.ezMovie.manage.service.SeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "seat")
@Tag(name = "Seat", description = "APIs for managing seats")
public class SeatController {
    @Autowired
    private SeatService seatService;

    @GetMapping("/{id}")
    @Operation(summary = "Get all seats by screenId", description = "Retrieve a list of all screens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách ghế được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách ghế.")
    })
    public ResponseEntity<List<SeatAdminDto>> getAllByScreen(
            @PathVariable int id,
            @Parameter(description = "Số trang để phân trang", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Kích thước mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(seatService.findAllByScreenId(id, pageable));
    }

    @GetMapping("/listSeat")
    @Operation(summary = "Get all seats by showtimeId", description = "Retrieve a list seat for showtime")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách ghế được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách ghế.")
    })
    public ResponseEntity<?> getAllByShowtime(
            @RequestParam Integer showtimeId,

            @Parameter(description = "Số trang để phân trang", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Kích thước mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        try {
            return ResponseEntity.ok(seatService.getSeatsByShowtimeId(showtimeId, pageable));
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return new ResponseEntity<>("Lỗi máy chủ khi thêm mới ghế", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/")
    @Operation(summary = "Create a new seat", description = "Add a new seat to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ghế được tạo thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu ghế không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo ghế.")
    })
    public ResponseEntity<?> create(@RequestBody CreateSeatRequest request) {
        try {
            seatService.createSeat(request);
            return ResponseEntity.ok("Thêm mới ghế thành công");
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return new ResponseEntity<>("Lỗi máy chủ khi thêm mới ghế", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/")
    @Operation(summary = "Update seat info", description = "Update seat Info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ghế được cập nhập thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu ghế không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo ghế.")
    })
    public ResponseEntity<?> update(@RequestBody UpdateSeatRequest request) {
        try {
            Seat updatedSeat = seatService.updateSeat(request);
            return ResponseEntity.ok(updatedSeat);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return new ResponseEntity<>("Lỗi máy chủ khi cập nhật ghế", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a seat", description = "Delete a seat by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ghế được xóa thành công."),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy ghế với ID đã cho."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi xóa ghế.")
    })
    public ResponseEntity<?> deleteSeat(@PathVariable int id) {
        seatService.deleteSeat(id);
        return ResponseEntity.ok("Xóa thành công");
    }

    @PostMapping("/update-redis")
    @Operation(summary = "Update Redis cache", description = "Update Redis cache when seats are modified")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật Redis thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi cập nhật Redis.")
    })
    public ResponseEntity<?> updateRedis(@RequestBody List<SeatDto> seats, @RequestParam Integer showtimeId) {
        try {
            System.out.println("Received Redis update request:");
            System.out.println("Showtime ID: " + showtimeId);
            System.out.println("Seats count: " + seats.size());
            System.out.println("Seats data: " + seats);
            
            seatService.updateRedisCache(seats, showtimeId);
            
            System.out.println("Redis update completed successfully for showtime: " + showtimeId);
            return ResponseEntity.ok("Cập nhật Redis thành công");
        } catch (Exception ex) {
            System.err.println("Error updating Redis: " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>("Lỗi khi cập nhật Redis: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/check-redis/{showtimeId}")
    @Operation(summary = "Check Redis cache", description = "Check Redis cache for a specific showtime")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis cache status retrieved successfully."),
            @ApiResponse(responseCode = "500", description = "Error checking Redis cache.")
    })
    public ResponseEntity<?> checkRedis(@PathVariable Integer showtimeId) {
        try {
            String redisKey = "listSeat::" + showtimeId;
            Object cachedData = seatService.getRedisTemplate().opsForValue().get(redisKey);
            
            System.out.println("Checking Redis cache:");
            System.out.println("Key: " + redisKey);
            System.out.println("Cached data: " + cachedData);
            
            return ResponseEntity.ok(Map.of(
                "key", redisKey,
                "exists", cachedData != null,
                "data", cachedData
            ));
        } catch (Exception ex) {
            System.err.println("Error checking Redis: " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>("Lỗi khi kiểm tra Redis: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
