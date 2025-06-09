package ezcloud.ezMovie.auth.service;

import ezcloud.ezMovie.admin.service.AdminService;
import ezcloud.ezMovie.auth.model.enities.CustomUserDetail;
import ezcloud.ezMovie.auth.model.enities.User;
import ezcloud.ezMovie.auth.model.payload.ChangePasswordRequest;
import ezcloud.ezMovie.auth.model.payload.JwtResponse;
import ezcloud.ezMovie.auth.model.payload.LoginRequest;
import ezcloud.ezMovie.auth.model.payload.RegisterRequest;
import ezcloud.ezMovie.auth.repository.UserRepository;
import ezcloud.ezMovie.exception.*;
import ezcloud.ezMovie.jwt.CodeGenerator;
import ezcloud.ezMovie.jwt.JwtService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
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
    @Autowired
    private UserRepository userRepository;

    public JwtResponse login(LoginRequest loginRequest) {
        // Thử đăng nhập với user trước
        try {
            if (userService.existsByEmail(loginRequest.getEmail())) {
                authenticateByEmail(loginRequest.getEmail(), loginRequest.getPassword());
                UserDetails userDetails = userService.loadUserByEmail(loginRequest.getEmail());
                String token = jwtService.generateToken(userDetails);
                return new JwtResponse(token);
            }
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid username or password");
        }
        throw new BadCredentialsException("Invalid username or password");
    }

    public void register(RegisterRequest request) throws MessagingException {
        if (userService.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistException("Username already exist: " + request.getUsername());
        }
        if (userService.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exist: " + request.getEmail());
        }

        if (!isValidEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Invalid email format");
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
    }

    public void verifyAccountRegister(String code) {
        User user = userService.findByVerificationCode(code);

        if (user == null) {
            throw new InvalidVerificationCodeException("Invalid verification code");
        }
        user.setVerified(true);
        user.setVerificationCode(null);
        userService.saveUser(user);
    }

    public void forgotPassword(String email) throws MessagingException {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new EmailAlreadyExistsException("Not found Email: " + email);
        }
        String resetCode = CodeGenerator.generateVerificationCode(6);
        user.setResetPasswordCode(resetCode);
        userService.saveUser(user);

        String body = "<d>Your password Reset Code is: </d> <h1  style=\\\"letter-spacing: 5px;\\> <strong>" + resetCode + "</strong></h1>";
        emailService.sendEmail(user.getEmail(), "Password Reset Code", body);
    }

    public void resetPassword(String resetCode, String newPassword) {
        User user = userService.findByResetPasswordCode(resetCode);
        if (user == null) {
            throw new InvalidResetCodeException("Invalid or expired reset code");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordCode(null);
        userService.saveUser(user);
    }

    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String email = jwtService.getEmailFromToken(token);

        User user = userService.findByEmail(email);
        if (user == null) {
            throw new EmailAlreadyExistsException("Not found inValid Email");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.saveUser(user);
    }

    private void authenticateByEmail(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    public JwtResponse loginGoogle(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(name);
            newUser.setVerified(true);
            newUser.setDeleted(false);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setRole("USER");
            newUser.setPassword(passwordEncoder.encode(String.valueOf(UUID.randomUUID())));
            userRepository.save(newUser);
        }

        CustomUserDetail userDetails = (CustomUserDetail) userService.loadUserByEmail(email);
        String token = jwtService.generateToken(userDetails);

        return new JwtResponse(token);
    }

}
