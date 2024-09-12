package ezcloud.ezMovie.controller;


import ezcloud.ezMovie.exception.EmailAlreadyExistsException;
import ezcloud.ezMovie.exception.UsernameAlreadyExistException;
import ezcloud.ezMovie.model.dto.ErrorResponse;
import ezcloud.ezMovie.model.payload.JwtResponse;
import ezcloud.ezMovie.model.payload.LoginRequest;
import ezcloud.ezMovie.model.payload.RegisterRequest;
import ezcloud.ezMovie.service.AuthService;
import lombok.RequiredArgsConstructor;
import ezcloud.ezMovie.model.payload.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về JWT token."),
            @ApiResponse(responseCode = "403", description = "Xác thực không thành công.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Unauthorized\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest jwtRequest) {
        try {
            JwtResponse response = authService.login(jwtRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error"));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Người dùng được đăng ký thành công."),
            @ApiResponse(responseCode = "400", description = "Tên người dùng hoặc email đã tồn tại.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Bad Request\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (UsernameAlreadyExistException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Username already exists: " + ex.getMessage()));
        } catch (EmailAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Email already exists: " + ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error"));
        }
    }

    @PostMapping("/verify-register")
    @Operation(summary = "Verify account registration", description = "Verify user account using the verification code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tài khoản đã được xác minh thành công."),
            @ApiResponse(responseCode = "400", description = "Mã xác thực không hợp lệ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Invalid verification code\" }")))
    })
    public ResponseEntity<?> verifyAccountRegister(@RequestParam("code") String code) {
        try {
            authService.verifyAccountRegister(code);
            return ResponseEntity.ok("Account verified successfully");
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid verification code"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error verifying account"));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send a password reset code to the user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mã xác thực quên mật khẩu đã được gửi."),
            @ApiResponse(responseCode = "404", description = "Email không tồn tại.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Email not found\" }")))
    })
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) throws MessagingException {
        try {
            authService.forgotPassword(email);
            return ResponseEntity.ok("Password reset code sent successfully");
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Email not found"));
        } catch (MessagingException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error sending password reset email"));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset user's password using the reset code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mật khẩu đã được đặt lại thành công."),
            @ApiResponse(responseCode = "400", description = "Mã xác thực không hợp lệ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Invalid or expired reset code\" }")))
    })
    public ResponseEntity<?> resetPassword(@RequestParam("resetCode") String resetCode,
                                           @RequestParam("newPassword") String newPassword) {
        try {
            authService.resetPassword(resetCode, newPassword);
            return ResponseEntity.ok("Password reset successfully");
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid or expired reset code"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error resetting password"));
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change user's password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mật khẩu đã được thay đổi thành công."),
            @ApiResponse(responseCode = "401", description = "Mật khẩu hiện tại không chính xác.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Incorrect current password\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        try {
            authService.changePassword(request, httpRequest);
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Incorrect current password"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error changing password"));
        }
    }
}
