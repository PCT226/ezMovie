package ezcloud.ezMovie.jwt;

import ezcloud.ezMovie.admin.auth.AdminUserDetails;
import ezcloud.ezMovie.auth.model.enities.CustomUserDetail;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    private final long EXPIRATION = 100000000000L;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof CustomUserDetail) {
            CustomUserDetail user = (CustomUserDetail) userDetails;
            claims.put("id", user.getUser().getId());
            claims.put("email", user.getUser().getEmail());
            claims.put("roles", user.getUser().getRole());
        } else if (userDetails instanceof AdminUserDetails) {
            AdminUserDetails admin = (AdminUserDetails) userDetails;
            claims.put("id", admin.getAdmin().getId());
            claims.put("email", admin.getAdmin().getEmail());
            claims.put("roles", admin.getAdmin().getRole());
        }
        return createToken(claims, userDetails.getUsername());
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String email = claims.get("email", String.class);
            logger.debug("Extracted email from token: {}", email);
            return email;
        } catch (Exception e) {
            logger.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String role = claims.get("roles", String.class);
            logger.debug("Extracted role from token: {}", role);
            return role;
        } catch (Exception e) {
            logger.error("Error extracting role from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String email = claims.get("email", String.class);
            boolean isValid = email.equals(userDetails.getUsername()) && !isTokenExpired(claims);
            logger.debug("Token validation result for {}: {}", email, isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        try {
            Date expiration = claims.getExpiration();
            boolean isExpired = expiration.before(new Date());
            logger.debug("Token expiration check: {}", isExpired);
            return isExpired;
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            logger.debug("Successfully parsed JWT claims");
            return claims;
        } catch (Exception e) {
            logger.error("Error parsing JWT claims: {}", e.getMessage());
            throw e;
        }
    }

    private String createToken(Map<String, Object> claims, String subject) {
        try {
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
            logger.debug("Successfully created JWT token for subject: {}", subject);
            return token;
        } catch (Exception e) {
            logger.error("Error creating JWT token: {}", e.getMessage());
            throw e;
        }
    }
}
