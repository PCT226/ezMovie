package ezcloud.ezMovie.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ezcloud.ezMovie.auth.model.payload.JwtResponse;
import ezcloud.ezMovie.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Autowired
    public CustomOAuth2SuccessHandler(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // Kiểm tra nếu Authentication là OAuth2AuthenticationToken
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = authToken.getPrincipal();

            // Gọi phương thức để tạo JWT
            JwtResponse jwtResponse = authService.loginGoogle(oAuth2User);

            response.setContentType("application/json");
            response.getWriter().write(new ObjectMapper().writeValueAsString(jwtResponse));
            response.setStatus(HttpServletResponse.SC_OK); // Trả về trạng thái 200 OK
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Trả về trạng thái 401 nếu không phải là OAuth2
        }
    }
}