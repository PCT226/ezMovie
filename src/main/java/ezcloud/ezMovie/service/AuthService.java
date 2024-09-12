package ezcloud.ezMovie.service;

import ezcloud.ezMovie.exception.EmailAlreadyExistsException;
import ezcloud.ezMovie.exception.UsernameAlreadyExistException;
import ezcloud.ezMovie.jwt.CodeGenerator;
import ezcloud.ezMovie.jwt.JwtService;
import ezcloud.ezMovie.model.enities.CustomUserDetail;
import ezcloud.ezMovie.model.enities.User;
import ezcloud.ezMovie.model.payload.ChangePasswordRequest;
import ezcloud.ezMovie.model.payload.JwtResponse;
import ezcloud.ezMovie.model.payload.LoginRequest;
import ezcloud.ezMovie.model.payload.RegisterRequest;
import ezcloud.ezMovie.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;

    public JwtResponse login(LoginRequest loginRequest) {
        try {
            authenticateByEmail(loginRequest.getEmail(), loginRequest.getPassword());
            UserDetails userDetails = userService.loadUserByEmail(loginRequest.getEmail());
            String token = jwtService.generateToken((CustomUserDetail) userDetails);
            return new JwtResponse(token);
        } catch (DisabledException e) {
            throw new RuntimeException("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new RuntimeException("INVALID_CREDENTIALS", e);
        }
    }

    public void register(RegisterRequest request) throws MessagingException {
        try {
            if (userService.existsByUsername(request.getUsername())) {
                throw new UsernameAlreadyExistException("Username already exists: " + request.getUsername());
            }
            if (userService.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
            }
            if (!isValidEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException("Invalid email format");
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole("USER");
            user.setPhoneNumber(request.getPhoneNumber());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            String verificationCode = CodeGenerator.generateVerificationCode(6);
            user.setVerificationCode(verificationCode);
            String body = "<d>Your verification code is: </d> <h1 style=\"letter-spacing: 5px;\"> <strong>" + verificationCode + "</strong></h1>";

            emailService.sendEmail(user.getEmail(), "Verification Code", body);
            userService.saveUser(user);
        } catch (MessagingException e) {
            throw new MessagingException("Error sending verification email", e);
        }
    }

    public void verifyAccountRegister(String code) {
        try {
            User user = userService.findByVerificationCode(code);
            if (user == null) {
                throw new UsernameNotFoundException("Invalid verification code");
            }
            user.setVerified(true);
            user.setVerificationCode(null);
            userService.saveUser(user);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying account", e);
        }
    }

    public void forgotPassword(String email) throws MessagingException {
        try {
            User user = userService.findByEmail(email);
            if (user == null) {
                throw new UsernameNotFoundException("Email not found: " + email);
            }
            String resetCode = CodeGenerator.generateVerificationCode(6);
            user.setResetPasswordCode(resetCode);
            userService.saveUser(user);

            String body = "<d>Your password reset code is: </d> <h1 style=\"letter-spacing: 5px;\"> <strong>" + resetCode + "</strong></h1>";
            emailService.sendEmail(user.getEmail(), "Password Reset Code", body);
        } catch (MessagingException e) {
            throw new MessagingException("Error sending password reset email", e);
        }
    }

    public void resetPassword(String resetCode, String newPassword) {
        try {
            User user = userService.findByResetPasswordCode(resetCode);
            if (user == null) {
                throw new UsernameNotFoundException("Invalid or expired reset code");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetPasswordCode(null);
            userService.saveUser(user);
        } catch (Exception e) {
            throw new RuntimeException("Error resetting password", e);
        }
    }

    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String email = jwtService.getEmailFromToken(token);

            User user = userService.findByEmail(email);
            if (user == null) {
                throw new UsernameNotFoundException("Email not found");
            }

            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new RuntimeException("Incorrect current password");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userService.saveUser(user);
        } catch (Exception e) {
            throw new RuntimeException("Error changing password", e);
        }
    }

    private void authenticateByEmail(String email, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (DisabledException e) {
            throw new RuntimeException("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new RuntimeException("INVALID_CREDENTIALS", e);
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }
}
