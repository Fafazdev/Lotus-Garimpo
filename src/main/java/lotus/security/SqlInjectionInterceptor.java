package lotus.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SqlInjectionInterceptor implements HandlerInterceptor {

    private final SqlInjectionGuard sqlInjectionGuard;

    public SqlInjectionInterceptor(SqlInjectionGuard sqlInjectionGuard) {
        this.sqlInjectionGuard = sqlInjectionGuard;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        request.getParameterMap().forEach((fieldName, values) -> {
            if (values == null) {
                return;
            }

            for (String value : values) {
                sqlInjectionGuard.validate(value, fieldName);
            }
        });

        return true;
    }
}