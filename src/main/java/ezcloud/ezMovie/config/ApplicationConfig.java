package ezcloud.ezMovie.config;

import ezcloud.ezMovie.jwt.JwtAuthFilter;
import ezcloud.ezMovie.jwt.JwtService;
import ezcloud.ezMovie.repository.UserRepository;
import ezcloud.ezMovie.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService, UserService userService) {
        return new JwtAuthFilter(jwtService, userService);
    }

    @Bean
    public JwtService jwtService() {
        return new JwtService();
    }

    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }
}
