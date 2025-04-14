package ezcloud.ezMovie.controller;


import ezcloud.ezMovie.manage.model.dto.CinemaDto;
import ezcloud.ezMovie.manage.model.dto.ShowtimeDto;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.manage.service.CinemaService;
import ezcloud.ezMovie.manage.service.ShowtimeService;
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

@RestController
@RequestMapping(value = "cinema")

@Tag(name = "Cinema", description = "APIs for managing cinemas")
public class CinemaController {

    @Autowired
    private CinemaService cinemaService;

    @Autowired
    private ShowtimeService showtimeService;

    @GetMapping("/")
    @Operation(summary = "Get all cinemas", description = "Retrieve a list of all cinemas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách rạp chiếu phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách rạp chiếu phim.")
    })
    public ResponseEntity<List<CinemaDto>> getAll(
            @Parameter(description = "Số trang để phân trang", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Kích thước mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cinemaService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cinemas info", description = "Cinema Info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chi tiết rạp chiếu phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy rạp chiếu phim.")
    })
    public ResponseEntity<Response<CinemaDto>> getById(@PathVariable int id) {
        return ResponseEntity.ok(cinemaService.getById(id));
    }

    @PostMapping("/")
    @Operation(summary = "Create a new cinema", description = "Add a new cinema to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rạp chiếu phim được tạo thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu rạp chiếu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo rạp chiếu phim.")
    })
    public ResponseEntity<Response<CinemaDto>> create(@RequestBody CinemaDto cinemaDto) {
        return ResponseEntity.ok(cinemaService.createCinema(cinemaDto));
    }

    @GetMapping("/showtime/{cinemaId}")
    public ResponseEntity<List<ShowtimeDto>> getShowtimesByCinema(@PathVariable Integer cinemaId) {
        List<ShowtimeDto> showtimes = showtimeService.getUpcomingAndOngoingShowtimes(cinemaId);
        return ResponseEntity.ok(showtimes);
    }

    @PutMapping("/")
    @Operation(summary = "Update cinema info", description = "Update Cinema Info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rạp chiếu phim được cập nhập thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu rạp chiếu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo rạp chiếu phim.")
    })
    public ResponseEntity<?> update(@RequestBody CinemaDto cinemaDto) {
        try {
            cinemaService.updateCinema(cinemaDto);
            return ResponseEntity.ok("Cập nhập thành công");
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a cinema", description = "Delete a cinema by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rạp chiếu phim được xóa thành công."),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy rạp chiếu phim với ID đã cho."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi xóa rạp chiếu phim.")
    })
    public ResponseEntity<?> deleteCinema(@PathVariable int id) {
        cinemaService.deleteCinema(id);
        return ResponseEntity.ok("Xóa thành công");

    }
}
