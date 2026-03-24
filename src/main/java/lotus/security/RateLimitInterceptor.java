package lotus.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/login", "/cadastro", "/api/usuarios/verificar-cpf"
    );

    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String uri = request.getRequestURI();
        if (!RATE_LIMITED_PATHS.contains(uri)) {
            return true;
        }

        String key = uri + ":" + getClientIp(request);

        if (!rateLimiter.isAllowed(key)) {
            if (isJsonRequest(request)) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"sucesso\":false,\"mensagem\":\"Muitas tentativas. Tente novamente em breve.\"}");
            } else {
                response.sendRedirect("/?erro=muitasTentativas");
            }
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }
}
