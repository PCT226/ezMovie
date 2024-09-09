package ezcloud.ezMovie.controller;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;


    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về JWT token."),
            @ApiResponse(responseCode = "401", description = "Xác thực không thành công.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Unauthorized\" }"))),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ.",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest jwtRequest) {
        // Thay đổi validate bằng email
        authenticateByEmail(jwtRequest.getEmail(), jwtRequest.getPassword());

        UserDetails user = userService.loadUserByEmail(jwtRequest.getEmail());

        String token = jwtService.generateToken((CustomUserDetail) user);

        return ResponseEntity.ok(new JwtResponse(token));
    }

    // Phương thức authenticateByEmail
    private void authenticateByEmail(String email, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (DisabledException e) {
            throw new RuntimeException("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new RuntimeException("INVALID_CREDENTIALS", e);
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
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) throws MessagingException {
        if (userService.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
        }
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
        }
        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Invalid email format");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Use request.getPassword()
        user.setRole("USER");
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        String verificationCode = CodeGenerator.generateVerificationCode(6);
        user.setVerificationCode(verificationCode);
        String body = "<d>Your verification code is: </d> <h1  style=\\\"letter-spacing: 5px;\\> <strong>" + verificationCode + "</strong></h1>";

        emailService.sendEmail(user.getEmail(), "Verification Code", body);
        userService.saveUser(user);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/verify-register")
    public ResponseEntity<String> verifyAccountRegister(@RequestParam("code") String code) {
        User user = userService.findByVerificationCode(code);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid verification code");
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        userService.saveUser(user);

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
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found");
        }

        // Tạo mã xác thực quên mật khẩu
        String resetCode = CodeGenerator.generateVerificationCode(6);;
        user.setResetPasswordCode(resetCode);
        userService.saveUser(user);

        // Gửi email mã xác thực
        String body = "<d>Your password Reset Code is: </d> <h1  style=\\\"letter-spacing: 5px;\\> <strong>" + resetCode + "</strong></h1>";
        emailService.sendEmail(user.getEmail(), "Password Reset Code", body);

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
        User user = userService.findByResetPasswordCode(resetCode);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired reset code");
        }

        // Đặt lại mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordCode(null);
        userService.saveUser(user);

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
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String email = jwtService.getEmailFromToken(token);

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email");
        }

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect current password");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.saveUser(user);

        return ResponseEntity.ok("Password changed successfully");
    }


    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }
}
