package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.manage.model.dto.ShowtimeDto;
import ezcloud.ezMovie.manage.model.payload.CreateShowtimeRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateShowtimeRq;
import ezcloud.ezMovie.manage.service.ShowtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<ShowtimeDto>> getAll(){
        return ResponseEntity.ok(showtimeService.getUpcomingShowtimes());
    }
    @GetMapping("/{movieId}")
    @Operation(summary = "Get all showtime available for movie", description = "Retrieve a list of all showtime available for movie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách lịch chiếu của phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách lịch chiếu phim.")
    })
    public ResponseEntity<List<ShowtimeDto>> getAll(@PathVariable Integer movieId){
        return ResponseEntity.ok(showtimeService.getUpcomingShowtimesForMovie(movieId));
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
    public ResponseEntity<?> deleteShowtime(@PathVariable int id){
        showtimeService.deleteShowtime(id);
        return ResponseEntity.ok("Xóa thành công");

    }
}