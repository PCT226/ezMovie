package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.manage.model.dto.ShowtimeDto;
import ezcloud.ezMovie.manage.model.payload.CreateShowtimeRequest;
import ezcloud.ezMovie.manage.model.payload.CreateBulkShowtimeRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateShowtimeRq;
import ezcloud.ezMovie.manage.service.ShowtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "showtime")
@Tag(name = "Showtime", description = "APIs for managing showtime")
public class ShowtimeController {
    @Autowired
    private ShowtimeService showtimeService;

    @GetMapping("/")
    @Operation(summary = "Get all showtime available", description = "Retrieve a list of all showtime available")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách lịch chiếu được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách lịch chiếu phim.")
    })
    public ResponseEntity<List<ShowtimeDto>> getAll() {
        return ResponseEntity.ok(showtimeService.getUpcomingShowtimes());
    }

    @GetMapping("/findShowtime")
    @Operation(summary = "Get all showtime available for movie", description = "Retrieve a list of all showtime available for movie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách lịch chiếu của phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách lịch chiếu phim.")
    })
    public ResponseEntity<List<ShowtimeDto>> getAll(
            @RequestParam Integer movieId,
            @RequestParam(required = false) Integer cinemaId,
            @Parameter(description = "The date in yyyy-MM-dd format", schema = @Schema(type = "string", format = "date"))
            @RequestParam(required = false) LocalDate date,

            @Parameter(description = "Số trang để phân trang", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(showtimeService.getUpcomingShowtimesForMovie(movieId, cinemaId, date, pageable));
    }


    @PostMapping("/")
    @Operation(summary = "Create a new showtime", description = "Add a new showtime to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lịch chiếu phim được tạo thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu lịch chiếu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo lịch chiếu phim.")
    })
    public ResponseEntity<?> create(@RequestBody CreateShowtimeRequest request) {
        try {
            showtimeService.createShowtime(request);
            return ResponseEntity.ok("Thêm mới lịch chiếu thành công");
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return new ResponseEntity<>("Lỗi máy chủ khi thêm mới lịch chiếu", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/")
    @Operation(summary = "Update showtime info", description = "Update showtime Info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lịch chiếu phim được cập nhập thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu lịch chiếu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo lịch chiếu phim.")
    })
    public ResponseEntity<?> update(@RequestBody UpdateShowtimeRq request) {
        try {
            ShowtimeDto updatedShowtime = showtimeService.updateShowtime(request);
            return ResponseEntity.ok(updatedShowtime);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return new ResponseEntity<>("Lỗi máy chủ khi cập nhật lịch chiếu", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a showtime", description = "Delete a showtime by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lịch chiếu phim được xóa thành công."),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lịch chiếu phim với ID đã cho."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi xóa lịch chiếu phim.")
    })
    public ResponseEntity<?> deleteShowtime(@PathVariable int id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.ok("Xóa thành công");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get showtime by ID", description = "Retrieve showtime details by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin lịch chiếu thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lịch chiếu với ID đã cho"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy thông tin lịch chiếu")
    })
    public ResponseEntity<ShowtimeDto> getShowtimeById(@PathVariable int id) {
        try {
            ShowtimeDto showtime = showtimeService.getShowtimeById(id);
            return ResponseEntity.ok(showtime);
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create multiple showtimes in date range", description = "Create showtimes for a date range with specific time slots and days of week")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo nhiều lịch chiếu thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo lịch chiếu.")
    })
    public ResponseEntity<?> createBulkShowtimes(@RequestBody CreateBulkShowtimeRequest request) {
        try {
            List<ShowtimeDto> createdShowtimes = showtimeService.createBulkShowtimes(request);
            return ResponseEntity.ok(createdShowtimes);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>("Lỗi máy chủ khi tạo lịch chiếu", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Get all showtimes for admin", description = "Retrieve all showtimes for admin management (including past showtimes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All showtimes retrieved successfully."),
            @ApiResponse(responseCode = "500", description = "Server error when retrieving showtimes.")
    })
    public ResponseEntity<List<ShowtimeDto>> getAllForAdmin(
            @Parameter(description = "Page number for pagination", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(showtimeService.getAllShowtimesForAdmin(pageable));
    }
}
