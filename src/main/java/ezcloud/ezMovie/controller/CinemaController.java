package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.model.enities.Cinema;
import ezcloud.ezMovie.service.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "cinema")
@Tag(name = "Cinema", description = "APIs for managing cinemas")
public class CinemaController {

    @Autowired
    private CinemaService cinemaService;

    @GetMapping("/")
    @Operation(summary = "Get all cinemas", description = "Retrieve a list of all cinemas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách rạp chiếu phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách rạp chiếu phim.")
    })
    public List<Cinema> getAll(){
        return cinemaService.getAll();
    }

    @PostMapping("/")
    @Operation(summary = "Create a new cinema", description = "Add a new cinema to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rạp chiếu phim được tạo thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu rạp chiếu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo rạp chiếu phim.")
    })
    public Cinema create(@RequestBody Cinema cinema){
        return cinemaService.createCinema(cinema);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a cinema", description = "Delete a cinema by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rạp chiếu phim được xóa thành công."),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy rạp chiếu phim với ID đã cho."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi xóa rạp chiếu phim.")
    })
    public void deleteCinema(@PathVariable int id){
        cinemaService.deleteCinema(id);
    }
}
