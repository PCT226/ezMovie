package ezcloud.ezMovie.auth.controller;

import ezcloud.ezMovie.auth.model.payload.ChangePasswordRequest;
import ezcloud.ezMovie.auth.model.payload.JwtResponse;
import ezcloud.ezMovie.auth.model.payload.LoginRequest;
import ezcloud.ezMovie.auth.model.payload.RegisterRequest;
import ezcloud.ezMovie.auth.service.AuthService;
import ezcloud.ezMovie.exception.EmailAlreadyExistsException;
import ezcloud.ezMovie.exception.EmailNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor

@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về JWT token.",
                    content = @Content(examples = @ExampleObject(value = "{ \"JWT token\":\"eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6IlVTRVIiLCJpZCI6IjFmNjVlZjVkLWZmM2MtNGI0OS1hZTBmLWJlNTkwMTk5YzU3YiIsImVtYWlsIjoiemFpaG9sZTIwMDNAZ21haWwuY29tIiwic3ViIjoiemFpaG9sZTIwMDNAZ21haWwuY29tIiwiaWF0IjoxNzI4ODkyNTU0LCJleHAiOjE4Mjg4OTI1NTR9.BRRDoznWu4J301MwIHZypQqcJAtWYB8O2Z8w0hgnUAE\"}"))),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Invalid request data\" }"))),
            @ApiResponse(responseCode = "401", description = "Sai tên đăng nhập hoặc mật khẩu.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Invalid username or password\" }"))),
            @ApiResponse(responseCode = "404", description = "Người dùng không tồn tại.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"User not found\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest jwtRequest) {
        try {
            JwtResponse jwtResponse = authService.login(jwtRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (EmailNotFoundException ex) {
            return new ResponseEntity<>("{ \"error\": \"User not found\" }", HttpStatus.NOT_FOUND);
        } catch (BadCredentialsException ex) {
            return new ResponseEntity<>("{ \"error\": \"Invalid username or password\" }", HttpStatus.UNAUTHORIZED);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>("{ \"error\": \"Invalid request data\" }", HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>("{ \"error\": \"Internal Server Error\" }", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Người dùng được đăng ký thành công.",
                    content = @Content(examples = @ExampleObject(value = "{\"success\":\"User registered successfully\"}"))),
            @ApiResponse(responseCode = "400", description = "Tên người dùng hoặc email đã tồn tại.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Bad Request\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
        } catch (Exception ex) {
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }


    @PostMapping("/verify-register")
    @Operation(summary = "Verify Account Registration",
            description = "Xác thực tài khoản đã đăng ký bằng mã xác thực.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tài khoản được xác thực thành công",
                    content = @Content(examples = @ExampleObject(value = "{\"error\":\"Account verified successfully\"}"))),
            @ApiResponse(responseCode = "400", description = "Mã xác thực không hợp lệ hoặc không tìm thấy",
                    content = @Content(examples = @ExampleObject(value = "{\"error\":\"Verify code incorrect\"}")))
    })
    public ResponseEntity<String> verifyAccountRegister(
            @Parameter(description = "Mã xác thực của tài khoản")
            @RequestParam("code") String code) {
        try {
            authService.verifyAccountRegister(code);
        } catch (Exception ex) {
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok("Account verified successfully");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send a password reset code to the user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mã xác thực quên mật khẩu đã được gửi.",
                    content = @Content(examples = @ExampleObject(value = "{\"success\": \"Password reset code sent successfully\" }"))),
            @ApiResponse(responseCode = "404", description = "Email không tồn tại.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Email not found\" }")))
    })
    public ResponseEntity<String> forgotPassword(@RequestParam("email") String email) {
        try {
            authService.forgotPassword(email);
        } catch (Exception ex) {
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok("Password reset code sent successfully");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Verify the password reset code and set a new password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mật khẩu đã được đặt lại thành công.",
                    content = @Content(examples = @ExampleObject(value = "{\"success\": \"Password reset successfully\" }"))),
            @ApiResponse(responseCode = "400", description = "Mã xác thực không hợp lệ hoặc đã hết hạn.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Invalid or expired reset code\" }")))
    })
    public ResponseEntity<String> resetPassword(@RequestParam("resetCode") String resetCode, @RequestParam("newPassword") String newPassword) {
        try {
            authService.resetPassword(resetCode, newPassword);
        } catch (UsernameNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change user password", description = "Change the password for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully.",
                    content = @Content(examples = @ExampleObject(value = "{ \"success\":\"Password changed successfully\" }"))),
            @ApiResponse(responseCode = "400", description = "Invalid input or incorrect current password.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Bad Request\" }"))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        try {
            authService.changePassword(request, httpRequest);
        } catch (EmailAlreadyExistsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (RuntimeException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok("Password changed successfully");
    }

    @GetMapping("/test")
    @Operation(summary = "Test authentication", description = "Test if the current token is valid")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Token is invalid")
    })
    public ResponseEntity<Map<String, Object>> testAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        
        if (authentication != null && authentication.isAuthenticated()) {
            response.put("authenticated", true);
            response.put("principal", authentication.getPrincipal().getClass().getSimpleName());
            response.put("authorities", authentication.getAuthorities());
            return ResponseEntity.ok(response);
        } else {
            response.put("authenticated", false);
            response.put("message", "No valid authentication found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

}
