package ezcloud.ezMovie.admin.service;

import ezcloud.ezMovie.admin.model.payload.AdminLoginRequest;
import ezcloud.ezMovie.admin.model.payload.AdminPasswordChangeRequest;
import ezcloud.ezMovie.admin.repository.AdminRepository;
import ezcloud.ezMovie.auth.model.payload.JwtResponse;
import ezcloud.ezMovie.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public JwtResponse login(AdminLoginRequest loginRequest) {
        try {
            // Kiểm tra xem email có tồn tại trong bảng admin không
            if (!adminRepository.existsByEmail(loginRequest.getEmail())) {
                System.out.println("Email not found in admin table");
                throw new BadCredentialsException("Invalid email or password");
            }

            // Thử xác thực với admin
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getEmail(),
                                loginRequest.getPassword()
                        )
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String jwt = jwtService.generateToken(userDetails);
                return new JwtResponse(jwt);
            } catch (BadCredentialsException e) {
                throw new BadCredentialsException("Invalid email or password");
            }
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public Object getProfile(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String email = jwtService.getEmailFromToken(token);
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    public void changePassword(AdminPasswordChangeRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        String email = jwtService.getEmailFromToken(token);
        
        var admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadCredentialsException("New passwords do not match");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
    }
} 