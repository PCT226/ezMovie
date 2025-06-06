package ezcloud.ezMovie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = {
    "ezcloud.ezMovie.auth.repository",
    "ezcloud.ezMovie.admin.repository",
    "ezcloud.ezMovie.booking.repository",
    "ezcloud.ezMovie.manage.repository"
})
@EnableTransactionManagement
public class JpaConfig {
} 