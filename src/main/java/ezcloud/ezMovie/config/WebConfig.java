package ezcloud.ezMovie.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "https://ezmovie-iota.vercel.app","https://ezmovie-production.up.railway.app")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .allowCredentials(true)
                .maxAge(3600);
    }
} 