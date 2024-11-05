package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.manage.model.dto.MovieInfo;
import ezcloud.ezMovie.manage.model.enities.Response;
import ezcloud.ezMovie.rateLimit.RateLimit;
import ezcloud.ezMovie.manage.service.MovieService;
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
@RequestMapping(value = "movie")
@Tag(name = "Movie", description = "APIs for managing movies")
public class MovieController {
    @Autowired
    private MovieService movieService;
    @GetMapping(value = "/")
    @RateLimit(ipCapacity = 10,ipRefillDuration = 60,ipRefillTokens = 5,systemCapacity = 100,systemRefillDuration = 60,systemRefillTokens = 50)
    @Operation(summary = "Get all movies", description = "Retrieve a list of all movies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách phim.")
    })
    public ResponseEntity<List<MovieInfo>> getListMovie(
            @Parameter(description = "Số trang để phân trang", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Kích thước mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(movieService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Movie info", description = "Movie Info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thông tin phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy thông tin phim.")
    })
    public ResponseEntity<?> getById(@PathVariable int id){
        return ResponseEntity.ok(movieService.findById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search movies", description = "Search movies by title, genre, and duration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách phim được tìm kiếm thành công."),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phim nào khớp với tiêu chí tìm kiếm."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tìm kiếm phim.")
    })
    public ResponseEntity<List<MovieInfo>> searchMovies(
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "genre", required = false) String genre,
            @RequestParam(name = "actor", required = false) String actor,
            @Parameter(description = "Số trang để phân trang", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<MovieInfo> movies = movieService.searchMovies(title, genre, actor,pageable);
        if (movies.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(movies);
    }

    @PostMapping("/")
    @Operation(summary = "Create a new movie", description = "Add a new movie to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "phim được tạo thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo phim.")
    })
    public ResponseEntity<Response<MovieInfo>> create(@RequestBody MovieInfo movieInfo){
        return ResponseEntity.ok(movieService.createMovie(movieInfo));
    }

    @PutMapping("/")
    @Operation(summary = "Update movie info", description = "Update movie Info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = " phim được cập nhập thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo phim.")
    })
    public ResponseEntity<Response<MovieInfo>> update(@RequestBody MovieInfo movieInfo){
        return ResponseEntity.ok(movieService.updateMovie(movieInfo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Movie", description = "Delete a Movie by it's ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = " phim được xóa thành công."),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phim với ID đã cho."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi xóa phim.")
    })
    public ResponseEntity<?> deleteMovie(@PathVariable int id){
        movieService.deleteMovie(id);
        return ResponseEntity.ok("Xóa thành công");

    }

}
