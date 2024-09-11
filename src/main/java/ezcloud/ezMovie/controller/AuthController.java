package ezcloud.ezMovie.controller;


import ezcloud.ezMovie.exception.EmailAlreadyExistsException;
import ezcloud.ezMovie.exception.UsernameAlreadyExistException;
import ezcloud.ezMovie.model.dto.UserInfo;
import ezcloud.ezMovie.model.payload.JwtResponse;
import ezcloud.ezMovie.model.payload.LoginRequest;
import ezcloud.ezMovie.model.payload.RegisterRequest;
import ezcloud.ezMovie.service.AuthService;
import ezcloud.ezMovie.service.UserService;
import lombok.RequiredArgsConstructor;
import ezcloud.ezMovie.jwt.CodeGenerator;
import ezcloud.ezMovie.jwt.JwtService;
import ezcloud.ezMovie.model.enities.CustomUserDetail;
import ezcloud.ezMovie.model.enities.User;
import ezcloud.ezMovie.model.payload.ChangePasswordRequest;
import ezcloud.ezMovie.model.payload.JwtResponse;
import ezcloud.ezMovie.model.payload.LoginRequest;
import ezcloud.ezMovie.model.payload.RegisterRequest;
import ezcloud.ezMovie.service.EmailService;
import ezcloud.ezMovie.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.regex.Pattern;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor

@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;


    @Autowired
    private EmailService emailService;


    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về JWT token."),
            @ApiResponse(responseCode = "403", description = "Xác thực không thành công.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Unauthorized\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest jwtRequest) {
        return ResponseEntity.ok(authService.login(jwtRequest));
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
    public ResponseEntity<?> register(@RequestBody RegisterRequest request){
        try {
            authService.register(request);
        } catch (UsernameAlreadyExistException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        } catch (EmailAlreadyExistsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ex) {
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }


    @PostMapping("/verify-register")
    public ResponseEntity<String> verifyAccountRegister(@RequestParam("code") String code) {
        try {
            authService.verifyAccountRegister(code);
        }catch (UsernameNotFoundException ex){
            return new ResponseEntity<>(ex.getMessage(),HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok("Account verified successfully");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send a password reset code to the user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mã xác thực quên mật khẩu đã được gửi."),
            @ApiResponse(responseCode = "404", description = "Email không tồn tại.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Email not found\" }")))
    })
    public ResponseEntity<String> forgotPassword(@RequestParam("email") String email) throws MessagingException {
        try{
            authService.forgotPassword(email);
        }catch (UsernameNotFoundException ex){
            return new ResponseEntity<>(ex.getMessage(),HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok("Password reset code sent successfully");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Verify the password reset code and set a new password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mật khẩu đã được đặt lại thành công."),
            @ApiResponse(responseCode = "400", description = "Mã xác thực không hợp lệ hoặc đã hết hạn.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Invalid or expired reset code\" }")))
    })
    public ResponseEntity<String> resetPassword(@RequestParam("resetCode") String resetCode, @RequestParam("newPassword") String newPassword) {
       try{
           authService.resetPassword(resetCode,newPassword);
       }catch (UsernameNotFoundException ex){
           return new ResponseEntity<>(ex.getMessage(),HttpStatus.NOT_FOUND);
       }
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change user password", description = "Change the password for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input or incorrect current password.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Bad Request\" }"))),
            @ApiResponse(responseCode = "500", description = "Internal server error.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        try {
            authService.changePassword(request,httpRequest);
        }catch (EmailAlreadyExistsException ex){
            return new ResponseEntity<>(ex.getMessage(),HttpStatus.NOT_FOUND);
        }catch (RuntimeException ex){
            return new ResponseEntity<>(ex.getMessage(),HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok("Password changed successfully");
    }

}
