package ezcloud.ezMovie.rateLimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class RedisRateLimiter {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:rateLimit:";
    private static final String BUCKET_PREFIX = "bucket:";

    // Cố định TTL cho mỗi bucket
    private static final long BUCKET_TTL_SECONDS = 60;

    private boolean acquireLock(String key) {
        String lockKey = LOCK_PREFIX + key;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(5));
        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(String key) {
        String lockKey = LOCK_PREFIX + key;
        redisTemplate.delete(lockKey);
    }

private boolean tryConsumeToken(String key, int capacity, int refillTokens, int refillDuration) {
    if (!acquireLock(key)) {
        return false; // Không thể lấy lock, bỏ qua request
    }
    try {
        String bucketKey = BUCKET_PREFIX + key;
        Map<Object, Object> bucketData = redisTemplate.opsForHash().entries(bucketKey);
        long currentTime = System.currentTimeMillis() / 1000; // Thời gian hiện tại (giây)

        // Lấy giá trị "lastRefillTime" từ Redis hoặc gán giá trị hiện tại nếu không có
        Long lastRefillTime = null;
        try {
            lastRefillTime = Long.parseLong(bucketData.getOrDefault("lastRefillTime", currentTime).toString());
        } catch (Exception e) {
            lastRefillTime = currentTime; // Gán giá trị hiện tại nếu không tìm thấy hoặc lỗi
        }

        // Lấy số lượng tokens hiện tại từ Redis hoặc gán giá trị mặc định nếu không có
        Integer tokens = null;
        try {
            tokens = Integer.parseInt(bucketData.getOrDefault("tokens", capacity).toString());
        } catch (Exception e) {
            tokens = capacity; // Gán capacity nếu không tìm thấy hoặc lỗi
        }

        long elapsedTime = currentTime - lastRefillTime;
        if (elapsedTime > 0) {
            int tokensToAdd = (int) Math.min(capacity, tokens + (elapsedTime * refillTokens) / refillDuration);
            tokens = Math.min(tokensToAdd, capacity);
            lastRefillTime = currentTime;
        }

        if (tokens > 0) {
            tokens--;
            redisTemplate.opsForHash().put(bucketKey, "tokens", tokens);
            redisTemplate.opsForHash().put(bucketKey, "lastRefillTime", lastRefillTime);

            // Thiết lập TTL cho bucket
            redisTemplate.expire(bucketKey, Duration.ofSeconds(BUCKET_TTL_SECONDS));

            return true; // Request được xử lý
        } else {
            return false; // Hết token
        }
    } finally {
        releaseLock(key);
    }
}

    public boolean rateLimit(String key, int capacity, int refillTokens, int refillDuration) {
        return tryConsumeToken(key, capacity, refillTokens, refillDuration);
    }
}

