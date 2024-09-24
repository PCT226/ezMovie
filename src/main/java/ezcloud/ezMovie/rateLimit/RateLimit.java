package ezcloud.ezMovie.rateLimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int ipCapacity() default 10; // Số lượng request cho IP
    int ipRefillTokens() default 10; // Số lượng token cho IP
    int ipRefillDuration() default 60; // Thời gian refill cho IP

    int systemCapacity() default 200; // Số lượng request cho toàn hệ thống
    int systemRefillTokens() default 100; // Số lượng token cho toàn hệ thống
    int systemRefillDuration() default 60; // Thời gian refill cho toàn hệ thống
}
