package ezcloud.ezMovie.auth.controller;

import ezcloud.ezMovie.auth.model.dto.UserUpdate;
import ezcloud.ezMovie.exception.EmailAlreadyExistsException;
import ezcloud.ezMovie.auth.model.dto.UserInfo;
import ezcloud.ezMovie.auth.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "user")
@Tag(name = "User", description = "APIs for managing user")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping(value = "/getAll")
    @ApiResponses(value = {

            @ApiResponse(responseCode = "200", description = "Lấy danh sách người dùng thành công",
                    content = @Content(examples = @ExampleObject(value = "\"[\n" +
                            "    {\n" +
                            "       \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                            "       \"username\": \"string\",\n" +
                            "       \"email\": \"string\",\n" +
                            "       \"phoneNumber\": \"string\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "       \"id\": \"2gb15f14-8719-4062-43gc-4c933f26hfu6\",\n" +
                            "       \"username\": \"string\",\n" +
                            "       \"email\": \"string\",\n" +
                            "       \"phoneNumber\": \"string\"\n" +
                            "    }\n" +
                            "]\" ")))
            ,
            @ApiResponse(responseCode = "500", description = "Lỗi nội bộ",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<List<UserInfo>> getAll(
            @Parameter(description = "Số trang để phân trang", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Kích thước mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try{
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(userService.getAll(pageable));
        }catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping(value = "/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy người dùng theo ID thành công",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                            "  \"username\": \"string\",\n" +
                            "  \"email\": \"string@string.com\",\n" +
                            "  \"phoneNumber\": \"string\"\n" +
                            "}"))),
            @ApiResponse(responseCode = "404", description = "Người dùng không tìm thấy",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"User not found with id: 3fa85f64-5717-4562-b3fc-2c963f66afa6\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi nội bộ",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(userService.findById(id));
        }catch (UsernameNotFoundException e){
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_FOUND);
        }catch (Exception ex) {
            return new ResponseEntity<>("Internal server error",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/update/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật người dùng thành công",
                    content = @Content(examples = @ExampleObject(value = "{\n" +
                            "  \"username\": \"string\",\n" +
                            "  \"phoneNumber\": \"string\"\n" +
                            " }"))),
            @ApiResponse(responseCode = "404", description = "Người dùng không tìm thấy",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"User not found with id: 3fa85f64-5717-4562-b3fc-2c963f66afa6\" }"))),
            @ApiResponse(responseCode = "400", description = "Email đã tồn tại hoặc các giá trị là null",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Value not null\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi nội bộ",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> updateUser(@PathVariable UUID id,@RequestBody UserUpdate userUpdate) {
        try {
            UserUpdate updatedUser = userService.updateUser(id,userUpdate);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (UsernameNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (EmailAlreadyExistsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value = "/delete/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa người dùng thành công",
                    content = @Content(examples = @ExampleObject(value = "{ \"success\": \"Delete success\n\" }"))),
            @ApiResponse(responseCode = "404", description = "Người dùng không tìm thấy",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"User not found with id: 3fa85f64-5717-4562-b3fc-2c963f66afa6\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi nội bộ",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }"))),
    })
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        try{
            userService.deleteUser(id);
            return ResponseEntity.ok("Delete success");
        }catch (UsernameNotFoundException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }catch (Exception ex) {
            return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
