package ezcloud.ezMovie.rateLimit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String ipKey = "ip:" + ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getRemoteAddr();
        String systemKey = "system";

        // Rate limit cho IP
        boolean ipAllowed = redisRateLimiter.rateLimit(ipKey, rateLimit.ipCapacity(), rateLimit.ipRefillTokens(), rateLimit.ipRefillDuration());
        if (!ipAllowed) {
            throw new RateLimitExceededException("Rate limit exceeded for IP");
        }


        // Rate limit cho toàn hệ thống
        boolean systemAllowed = redisRateLimiter.rateLimit(systemKey, rateLimit.systemCapacity(), rateLimit.systemRefillTokens(), rateLimit.systemRefillDuration());
        if (!systemAllowed) {
            throw new RateLimitExceededException("Rate limit exceeded for System");
        }

        return joinPoint.proceed();
    }
}

