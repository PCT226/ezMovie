package ezcloud.ezMovie.jwt;

import ezcloud.ezMovie.admin.service.AdminService;
import ezcloud.ezMovie.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;
    private final AdminService adminService;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/v1/auth/",
            "/api/v1/admin/auth/",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/auth/",
            "/login",
            "/oauth2/",
            "/payment/",
            "/ticket/",
            "/movie/",
            "/cinema/",
            "/showtime/",
            "/api/chat/",
            "/ws/",
            "/seat/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            
            // Skip token validation for public paths
            if (isPublicPath(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = jwtService.getEmailFromToken(token);
                
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = null;
                    
                    // Kiểm tra role từ token
                    String role = jwtService.getRoleFromToken(token);
                    
                    if ("ADMIN".equals(role)) {
                        try {
                            userDetails = adminService.loadUserByUsername(email);
                        } catch (Exception e) {
                            logger.error("Failed to load admin details: " + e.getMessage());
                        }
                    } else {
                        try {
                            userDetails = userService.loadUserByEmail(email);
                        } catch (Exception e) {
                            logger.error("Failed to load user details: " + e.getMessage());
                        }
                    }

                    if (userDetails != null && jwtService.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error processing JWT token: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/chat/") || path.startsWith("/ws/");
    }
}
