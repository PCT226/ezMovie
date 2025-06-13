package ezcloud.ezMovie.jwt;

import ezcloud.ezMovie.admin.service.AdminService;
import ezcloud.ezMovie.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtService jwtService;
    private final UserService userService;
    private final AdminService adminService;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/admin/auth/login",
        "/swagger-ui",
        "/v3/api-docs"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getServletPath();
            logger.debug("Processing request for path: {}", path);
            
            if (isPublicPath(path)) {
                logger.debug("Public path, skipping authentication");
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");
            logger.debug("Auth header: {}", authHeader != null ? "present" : "null");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.debug("No Bearer token found in request for path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            logger.debug("Extracted token, length: {}", token.length());
            
            String email = jwtService.getEmailFromToken(token);
            logger.debug("Extracted email from token: {}", email);
            
            if (email == null) {
                logger.error("Could not extract email from token for path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                logger.debug("No existing authentication, loading user details for email: {}", email);
                UserDetails userDetails = loadUserDetails(email);
                logger.debug("User details loaded: {}", userDetails != null ? "success" : "failed");
                
                if (userDetails != null) {
                    logger.debug("Validating token for user: {}", userDetails.getUsername());
                    logger.debug("User details class: {}", userDetails.getClass().getSimpleName());
                    logger.debug("User details: username={}, authorities={}", 
                        userDetails.getUsername(), userDetails.getAuthorities());
                    
                    boolean tokenValid = jwtService.validateToken(token, userDetails);
                    logger.debug("Token validation result: {}", tokenValid);
                    
                    if (tokenValid) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.debug("Successfully authenticated user: {} for path: {}", email, path);
                    } else {
                        logger.error("Token validation failed for user: {} for path: {}", email, path);
                    }
                } else {
                    logger.error("Failed to load user details for email: {} for path: {}", email, path);
                }
            } else {
                logger.debug("Authentication already exists for path: {}", path);
            }
        } catch (Exception e) {
            logger.error("Error processing JWT token for path {}: {}", request.getServletPath(), e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private UserDetails loadUserDetails(String email) {
        logger.debug("Loading user details for email: {}", email);
        
        try {
            logger.debug("Trying to load as admin first...");
            UserDetails adminDetails = adminService.loadUserByUsername(email);
            logger.debug("Successfully loaded admin details for: {}", email);
            return adminDetails;
        } catch (Exception e) {
            logger.debug("Failed to load as admin for email {}: {}", email, e.getMessage());
            try {
                logger.debug("Trying to load as user...");
                UserDetails userDetails = userService.loadUserByEmail(email);
                logger.debug("Successfully loaded user details for: {}", email);
                return userDetails;
            } catch (Exception ex) {
                logger.error("User not found: {} - Admin error: {}, User error: {}", email, e.getMessage(), ex.getMessage());
                return null;
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return isPublicPath(path);
    }
}
