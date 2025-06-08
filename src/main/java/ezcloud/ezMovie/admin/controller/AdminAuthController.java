package ezcloud.ezMovie.admin.controller;

import ezcloud.ezMovie.admin.model.entities.Admin;
import ezcloud.ezMovie.admin.model.payload.AdminLoginRequest;
import ezcloud.ezMovie.admin.model.payload.AdminPasswordChangeRequest;
import ezcloud.ezMovie.admin.service.AdminAuthService;
import ezcloud.ezMovie.admin.service.AdminService;
import ezcloud.ezMovie.auth.model.payload.JwtResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "APIs for admin authentication and profile management")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminService adminService;

    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Login with admin credentials")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(examples = @ExampleObject(value = "{\"message\": \"Invalid email or password\"}")))
    })
    public ResponseEntity<JwtResponse> login(@RequestBody AdminLoginRequest loginRequest) {
        try {
            JwtResponse response = adminAuthService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/profile")
    @Operation(summary = "Get admin profile", description = "Get the profile information of the logged-in admin")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(examples = @ExampleObject(value = "{\"id\":\"uuid\",\"email\":\"admin@example.com\",\"role\":\"ADMIN\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Unauthorized\" }"))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(adminAuthService.getProfile(request));
        } catch (Exception ex) {
            return new ResponseEntity<>("{ \"error\": \"Internal Server Error\" }", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change admin password", description = "Change the password for the logged-in admin")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully",
                    content = @Content(examples = @ExampleObject(value = "{ \"success\": \"Password changed successfully\" }"))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Invalid current password\" }"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Unauthorized\" }"))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(examples = @ExampleObject(value = "{ \"error\": \"Internal Server Error\" }")))
    })
    public ResponseEntity<?> changePassword(@RequestBody AdminPasswordChangeRequest request, HttpServletRequest httpRequest) {
        try {
            adminAuthService.changePassword(request, httpRequest);
            return ResponseEntity.ok("Password changed successfully");
        } catch (BadCredentialsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>("{ \"error\": \"Internal Server Error\" }", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/staff")
    @Operation(summary = "Add new admin staff", description = "Create a new admin staff member")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Admin staff created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> addStaff(@RequestBody Admin admin) {
        try {
            Admin createdAdmin = adminService.createAdmin(admin);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAdmin);
        } catch (Exception ex) {
            return new ResponseEntity<>("{ \"error\": \"Internal Server Error\" }", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 