package ezcloud.ezMovie.controller;

import ezcloud.ezMovie.manage.model.dto.ScreenDto;
import ezcloud.ezMovie.manage.model.enities.Screen;
import ezcloud.ezMovie.manage.model.payload.CreateScreenRequest;
import ezcloud.ezMovie.manage.model.payload.UpdateScreenRequest;
import ezcloud.ezMovie.manage.service.ScreenService;
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
@RequestMapping(value = "screen")
@Tag(name = "Screen", description = "APIs for managing screens")
public class ScreenController {
    @Autowired
    private ScreenService screenService;

    @GetMapping("/")
    @Operation(summary = "Get all screens", description = "Retrieve a list of all screens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Danh sách phòng chiếu phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách phòng chiếu phim.")
    })
    public ResponseEntity<List<ScreenDto>> getAll(){
        return ResponseEntity.ok(screenService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get screens info", description = "Screen Info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chi tiết phòng chiếu phim được lấy thành công."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy phòng chiếu phim.")
    })
    public ResponseEntity<Screen> getById(@PathVariable int id){
        return ResponseEntity.ok(screenService.findById(id));
    }

    @PostMapping("/")
    @Operation(summary = "Create a new screen", description = "Add a new screen to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phòng chiếu phim được tạo thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu phòng chiếu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo phòng chiếu phim.")
    })
    public ResponseEntity<?> create(@RequestBody CreateScreenRequest request) {
        try {
            screenService.createScreen(request);
            return ResponseEntity.ok("Thêm mới phòng chiếu thành công");
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return new ResponseEntity<>("Lỗi máy chủ khi thêm mới phòng chiếu", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/")
    @Operation(summary = "Update screen info", description = "Update screen Info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phòng chiếu phim được cập nhập thành công."),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ. Dữ liệu phòng chiếu phim không hợp lệ hoặc bị thiếu."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo phòng chiếu phim.")
    })
    public ResponseEntity<?> update(@RequestBody UpdateScreenRequest request) {
        try {
            Screen updatedScreen = screenService.updateScreen(request);
            return ResponseEntity.ok(updatedScreen);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return new ResponseEntity<>("Lỗi máy chủ khi cập nhật phòng chiếu", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a screen", description = "Delete a screen by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phòng chiếu phim được xóa thành công."),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phòng chiếu phim với ID đã cho."),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi xóa phòng chiếu phim.")
    })
    public ResponseEntity<?> deleteScreen(@PathVariable int id){
        screenService.deleteScreen(id);
        return ResponseEntity.ok("Xóa thành công");

    }
}
