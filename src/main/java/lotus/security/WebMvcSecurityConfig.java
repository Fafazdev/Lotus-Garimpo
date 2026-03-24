package lotus.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    private final SqlInjectionInterceptor sqlInjectionInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcSecurityConfig(SqlInjectionInterceptor sqlInjectionInterceptor,
                                RateLimitInterceptor rateLimitInterceptor) {
        this.sqlInjectionInterceptor = sqlInjectionInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
        registry.addInterceptor(sqlInjectionInterceptor);
    }
}