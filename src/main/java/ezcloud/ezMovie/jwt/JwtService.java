package ezcloud.ezMovie.jwt;

import ezcloud.ezMovie.auth.model.enities.CustomUserDetail;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Sử dụng phương thức để tạo khóa bí mật an toàn
    private final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private final long EXPIRATION = 100000000000L;


    public String generateToken(CustomUserDetail user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getUser().getId());
        claims.put("email", user.getUser().getEmail());
        claims.put("roles", user.getUser().getRole());
        return createToken(claims, user.getEmail());
    }

    public Boolean validateToken(String token, CustomUserDetail userDetails) {
        // Lấy ngày hết hạn từ token
        Date expirationDate = getExpirationDateFromToken(token);

        // Kiểm tra token đã hết hạn chưa
        if (expirationDate.before(new Date())) {
            return false;
        }

        // Lấy email từ token
        String email = getEmailFromToken(token);

        // Kiểm tra email trong token có khớp với tên người dùng và token chưa hết hạn
        return userDetails.getEmail().equals(email);
    }


    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String createToken(Map<String, Object> claims, String email) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }
}
