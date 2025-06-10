package ezcloud.ezMovie.config;

import ezcloud.ezMovie.admin.repository.AdminRepository;
import ezcloud.ezMovie.admin.service.AdminService;
import ezcloud.ezMovie.auth.repository.UserRepository;
import ezcloud.ezMovie.auth.service.UserService;
import ezcloud.ezMovie.jwt.JwtAuthFilter;
import ezcloud.ezMovie.jwt.JwtService;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class ApplicationConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public JwtService jwtService() {
        return new JwtService();
    }

    @Bean
    public AdminService adminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        return new AdminService(adminRepository, passwordEncoder);
    }

    @Bean
    @Lazy
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService, UserService userService, AdminService adminService) {
        return new JwtAuthFilter(jwtService, userService, adminService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
