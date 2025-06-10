package ezcloud.ezMovie.admin.service;

import ezcloud.ezMovie.admin.model.payload.AdminLoginRequest;
import ezcloud.ezMovie.admin.model.payload.AdminPasswordChangeRequest;
import ezcloud.ezMovie.admin.repository.AdminRepository;
import ezcloud.ezMovie.auth.model.payload.JwtResponse;
import ezcloud.ezMovie.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthService.class);

    private final AdminRepository adminRepository;
    private final AuthenticationManager adminAuthenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public JwtResponse login(AdminLoginRequest loginRequest) {
        logger.info("Attempting admin login for email: {}", loginRequest.getEmail());
        
        try {
            if (!adminRepository.existsByEmail(loginRequest.getEmail())) {
                logger.error("Email not found in admin table: {}", loginRequest.getEmail());
                throw new BadCredentialsException("Invalid email or password");
            }

            Authentication authentication = adminAuthenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            if (authentication != null && authentication.isAuthenticated()) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String jwt = jwtService.generateToken(userDetails);
                logger.info("Admin login successful for email: {}", loginRequest.getEmail());
                return new JwtResponse(jwt);
            }
            
            logger.error("Authentication failed for admin: {}", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        } catch (BadCredentialsException e) {
            logger.error("Bad credentials for admin: {}", loginRequest.getEmail());
            throw e;
        } catch (Exception e) {
            logger.error("Error during admin login: {}", e.getMessage(), e);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public Object getProfile(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String email = jwtService.getEmailFromToken(token);
        logger.info("Getting profile for admin: {}", email);
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    public void changePassword(AdminPasswordChangeRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        String email = jwtService.getEmailFromToken(token);
        logger.info("Attempting password change for admin: {}", email);
        
        var admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            logger.error("Current password incorrect for admin: {}", email);
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            logger.error("New passwords do not match for admin: {}", email);
            throw new BadCredentialsException("New passwords do not match");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
        logger.info("Password changed successfully for admin: {}", email);
    }
} 